package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/**
 * 纯客户端锁定喵：
 *  - 准星锥体内（默认 ±8°）最靠近准星的实体，保持 3 秒自动锁定
 *  - X 键立刻锁 / 取消锁定
 *  - 目标离开视野（±55°）满 8 秒自动脱锁
 * 注意：纯视觉，不会给任何武器提供制导喵😼
 */
public final class RwrLock {

    /** 锁定距离（格喵） */
    public static double range = 128.0;
    /** 索敌锥角（度喵） */
    public static double cone = 8.0;
    /** 保持多久自动锁定（tick 喵） */
    public static int holdTicks = 60;
    /** 视野外多久脱锁（tick 喵） */
    public static int outOfViewTicks = 160;
    /** 视野判定锥角（度喵） */
    public static double viewCone = 55.0;

    private static UUID lockedId;
    private static UUID candidateId;
    private static int hold = 0;
    private static int outOfView = 0;

    private RwrLock() {}

    public static void setRange(double v) { range = clamp(v, 8.0, 512.0); }
    public static void setCone(double v) { cone = clamp(v, 1.0, 60.0); }
    public static void setHold(int v) { holdTicks = Math.max(0, v); }
    public static void setOutOfView(int v) { outOfViewTicks = Math.max(0, v); }
    public static void setViewCone(double v) { viewCone = clamp(v, 5.0, 120.0); }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** 每 tick 调用（客户端喵） */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            release();
            return;
        }
        Player self = mc.player;
        Vec3 eye = self.getEyePosition();
        Vec3 look = self.getLookAngle();

        // ---- 已锁定：维持判定 ----
        if (lockedId != null) {
            Entity e = find(lockedId);
            if (e == null || !e.isAlive()) {
                release();
            } else {
                Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
                boolean inView = d.lengthSqr() > 1.0E-4
                        && d.normalize().dot(look) >= Math.cos(Math.toRadians(viewCone));
                if (inView) {
                    outOfView = 0;
                } else if (++outOfView >= outOfViewTicks) {
                    release();
                    return;
                }
                return;
            }
        }

        // ---- 候选：准星锥体内最靠近准星的实体喵 ----
        Entity cand = pickCandidate(self, eye, look);
        UUID candId = cand == null ? null : cand.getUUID();
        if (candId == null) {
            candidateId = null;
            hold = 0;
            return;
        }
        if (candId.equals(candidateId)) {
            hold++;
        } else {
            candidateId = candId;
            hold = 1;
        }
        if (hold >= holdTicks) {
            lockedId = candId;
            outOfView = 0;
        }
    }

    /** X 键喵：有锁定就取消，没有就立刻锁当前候选喵 */
    public static boolean toggle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        if (lockedId != null) {
            release();
            return false;
        }
        Entity cand = pickCandidate(mc.player, mc.player.getEyePosition(), mc.player.getLookAngle());
        if (cand == null) return false;
        lockedId = cand.getUUID();
        candidateId = lockedId;
        hold = holdTicks;
        outOfView = 0;
        return true;
    }

    public static void release() {
        lockedId = null;
        candidateId = null;
        hold = 0;
        outOfView = 0;
    }

    /** 当前锁定目标（客户端实体喵） */
    public static Entity locked() {
        return lockedId == null ? null : find(lockedId);
    }

    /** 自动锁定进度 0~1 喵 */
    public static float progress() {
        if (lockedId != null) return 1.0f;
        return holdTicks <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, hold / (float) holdTicks));
    }

    private static Entity find(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(id)) return e;
        }
        return null;
    }

    /** 准星锥体内、最接近视线中心且在范围内的目标喵 */
    private static Entity pickCandidate(Player self, Vec3 eye, Vec3 look) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        double cosCone = Math.cos(Math.toRadians(cone));
        Entity best = null;
        double bestCos = cosCone;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == self || !e.isAlive()) continue;
            double dist = self.distanceTo(e);
            if (dist > range) continue;
            if (!(e instanceof LivingEntity) && !(e instanceof net.minecraft.world.entity.projectile.Projectile)) continue;

            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            double len = d.length();
            if (len < 0.2) continue;
            double cos = d.dot(look) / len;
            if (cos <= bestCos) continue;
            bestCos = cos;
            best = e;
        }
        return best;
    }

    /** 供 HUD 用：距离喵 */
    public static double distanceTo(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == null) return 0.0;
        return mc.player.distanceTo(e);
    }

    /** 供 HUD 用：目标速度（格/秒喵） */
    public static double speedOf(Entity e) {
        return e == null ? 0.0 : e.getDeltaMovement().length() * 20.0;
    }

    static {
        // 让 IDE 知道 Comparator 被用到（保持导入整洁喵）
        Comparator<Entity> ignored = Comparator.comparingDouble(RwrLock::distanceTo);
    }
}
