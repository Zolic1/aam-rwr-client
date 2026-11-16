package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯客户端索敌喵：只用「客户端已经知道的实体」（受实体追踪距离限制喵）。
 *
 * 关键点：光点保存「相对眼睛的世界坐标偏移（dx/dy/dz）」，
 * 由 HUD 每帧用当前相机基向量投影，视角转动时不会偏移喵。
 */
public final class RwrScanner {

    public static final byte TYPE_PLAYER = 0;
    public static final byte TYPE_PROJECTILE = 1;
    public static final byte TYPE_HOSTILE = 2;    // 敌对生物（含猪灵喵）
    public static final byte TYPE_NEUTRAL = 3;    // 中立 / 友好生物喵
    public static final byte TYPE_ITEM = 4;       // 掉落物喵

    /** 一个雷达光点喵（dx/dy/dz = 相对眼睛的世界偏移喵） */
    public record Contact(double bearing, double elev, double dist, byte type,
                          boolean friendly, boolean closing,
                          double dx, double dy, double dz, String name) {}

    public record Scan(List<Contact> contacts, boolean threat, boolean helmet) {}

    private RwrScanner() {}

    public static boolean wearingHelmet(Player player) {
        var stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (stack.isEmpty()) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.getPath().contains("rwr");
    }

    public static Scan scan() {
        Minecraft mc = Minecraft.getInstance();
        List<Contact> out = new ArrayList<>();
        if (mc.player == null || mc.level == null) {
            return new Scan(out, false, false);
        }

        Player self = mc.player;
        boolean helmet = wearingHelmet(self);
        Vec3 eye = self.getEyePosition();
        Vec3 look = self.getLookAngle();
        var cam = mc.gameRenderer.getMainCamera();
        Vec3 forward = (cam != null && cam.isInitialized()) ? new Vec3(cam.forwardVector()) : look;
        Vec3 up = (cam != null && cam.isInitialized()) ? new Vec3(cam.upVector()) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = (cam != null && cam.isInitialized()) ? new Vec3(cam.leftVector()).scale(-1.0)
                : forward.cross(new Vec3(0.0, 1.0, 0.0)).normalize();

        boolean threat = false;
        double range = RwrConfig.range;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == self || !e.isAlive()) continue;

            boolean isPlayer = e instanceof Player;
            boolean isProjectile = e instanceof Projectile || isMissileByName(e);
            boolean isHostile = false;
            boolean isNeutral = false;
            boolean isItem = false;

            if (!isPlayer && !isProjectile && RwrConfig.allEntities) {
                if (e instanceof net.minecraft.world.entity.item.ItemEntity) {
                    isItem = true;
                } else if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    // 敌对：Monster（僵尸/骷髅/苦力怕…喵）或 Enemy（包括猪灵喵）
                    boolean hostile = e instanceof net.minecraft.world.entity.monster.Monster
                            || e instanceof net.minecraft.world.entity.monster.Enemy;
                    if (hostile) isHostile = true; else isNeutral = true;
                }
            }
            if (!isPlayer && !isProjectile && !isHostile && !isNeutral && !isItem) continue;

            double dist = self.distanceTo(e);
            if (dist > range) continue;

            Vec3 rel = e.getBoundingBox().getCenter().subtract(eye);
            double len = rel.length();
            if (len < 0.05) continue;

            double x = rel.dot(right);
            double y = rel.dot(up);
            double z = rel.dot(forward);
            double horiz = Math.sqrt(x * x + z * z);

            double bearing = Math.toDegrees(Math.atan2(x, z));
            double elev = Math.toDegrees(Math.atan2(y, Math.max(0.001, horiz)));

            boolean friendly = isPlayer && self.isAlliedTo(e);
            boolean closing = false;

            if (isProjectile) {
                Vec3 toMe = self.position().subtract(e.position());
                double dl = toMe.length();
                if (dl > 0.01) {
                    Vec3 vel = e.getDeltaMovement().scale(20.0);
                    double sp = vel.length();
                    if (sp > 0.5) {
                        closing = vel.dot(toMe.scale(1.0 / dl)) / sp > 0.7;
                    }
                }
                boolean near = RwrConfig.threatRange <= 0.0 || dist <= RwrConfig.threatRange;
                if (closing && near && !friendly) {
                    threat = true;
                }
            }

            byte type = isPlayer ? TYPE_PLAYER
                    : isProjectile ? TYPE_PROJECTILE
                    : isHostile ? TYPE_HOSTILE
                    : isNeutral ? TYPE_NEUTRAL
                    : TYPE_ITEM;
            out.add(new Contact(bearing, elev, dist, type,
                    friendly, closing, rel.x, rel.y, rel.z, e.getName().getString()));
        }

        return new Scan(out, threat, helmet);
    }

    /** 实体 id 里带 missile 的也算（兼容其它模组的导弹喵） */
    private static boolean isMissileByName(Entity e) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key != null && key.getPath().contains("missile");
    }
}
