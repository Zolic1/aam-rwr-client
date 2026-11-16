package dev.aam.rwr;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 血量 / 护甲显示喵：
 *  - 原版生物有标准最大血量表喵；服务端（插件/数据包）改过最大血量时，
 *    显示会和原版差很多 → 这时弃用服务端数值，按「原版满血」建本地模型喵
 *  - 本地模型只跟随观察到的血量变化（掉血/回血），避免被服务端异常数值带偏喵
 */
public final class RwrHealth {

    /** 常见原版生物的最大血量喵（没列出来的用服务端值喵） */
    private static final Map<EntityType<?>, Float> VANILLA_MAX = new HashMap<>();

    static {
        reg(EntityType.ZOMBIE, 20f);
        reg(EntityType.ZOMBIE_VILLAGER, 20f);
        reg(EntityType.HUSK, 20f);
        reg(EntityType.DROWNED, 20f);
        reg(EntityType.SKELETON, 20f);
        reg(EntityType.STRAY, 20f);
        reg(EntityType.BOGGED, 16f);
        reg(EntityType.WITHER_SKELETON, 20f);
        reg(EntityType.CREEPER, 20f);
        reg(EntityType.SPIDER, 16f);
        reg(EntityType.CAVE_SPIDER, 12f);
        reg(EntityType.ENDERMAN, 40f);
        reg(EntityType.PIGLIN, 16f);
        reg(EntityType.PIGLIN_BRUTE, 50f);
        reg(EntityType.ZOMBIFIED_PIGLIN, 20f);
        reg(EntityType.HOGLIN, 40f);
        reg(EntityType.ZOGLIN, 40f);
        reg(EntityType.BLAZE, 20f);
        reg(EntityType.GHAST, 10f);
        reg(EntityType.MAGMA_CUBE, 16f);
        reg(EntityType.SLIME, 16f);
        reg(EntityType.WITCH, 26f);
        reg(EntityType.PILLAGER, 24f);
        reg(EntityType.VINDICATOR, 24f);
        reg(EntityType.EVOKER, 24f);
        reg(EntityType.RAVAGER, 100f);
        reg(EntityType.WARDEN, 500f);
        reg(EntityType.WITHER, 300f);
        reg(EntityType.ENDER_DRAGON, 200f);
        reg(EntityType.PLAYER, 20f);
        reg(EntityType.COW, 10f);
        reg(EntityType.PIG, 10f);
        reg(EntityType.SHEEP, 8f);
        reg(EntityType.CHICKEN, 4f);
        reg(EntityType.RABBIT, 3f);
        reg(EntityType.HORSE, 30f);
        reg(EntityType.VILLAGER, 20f);
        reg(EntityType.WANDERING_TRADER, 20f);
        reg(EntityType.IRON_GOLEM, 100f);
        reg(EntityType.SNOW_GOLEM, 4f);
        reg(EntityType.WOLF, 8f);
        reg(EntityType.CAT, 10f);
        reg(EntityType.FOX, 10f);
        reg(EntityType.BEE, 10f);
        reg(EntityType.ALLAY, 20f);
        reg(EntityType.GOAT, 10f);
        reg(EntityType.AXOLOTL, 14f);
        reg(EntityType.DOLPHIN, 10f);
        reg(EntityType.POLAR_BEAR, 30f);
        reg(EntityType.PANDA, 20f);
        reg(EntityType.LLAMA, 30f);
        reg(EntityType.TRADER_LLAMA, 30f);
        reg(EntityType.SHULKER, 30f);
        reg(EntityType.GUARDIAN, 30f);
        reg(EntityType.ELDER_GUARDIAN, 80f);
        reg(EntityType.PHANTOM, 20f);
        reg(EntityType.VEX, 14f);
        reg(EntityType.SILVERFISH, 8f);
        reg(EntityType.ENDERMITE, 8f);
        reg(EntityType.BREEZE, 30f);
        reg(EntityType.BAT, 6f);
        reg(EntityType.PARROT, 6f);
        reg(EntityType.SQUID, 10f);
        reg(EntityType.GLOW_SQUID, 10f);
        reg(EntityType.COD, 3f);
        reg(EntityType.SALMON, 3f);
        reg(EntityType.TROPICAL_FISH, 3f);
    }

    private static void reg(EntityType<?> type, float hp) {
        VANILLA_MAX.put(type, hp);
    }

    /** 显示用的血量数据喵 */
    public record Display(float hp, float max, int armor, boolean localModel) {}

    private static final class Model {
        float hp;
        float max;
        int lastHurt;
    }

    private static final Map<UUID, Model> MODELS = new HashMap<>();
    private static final Map<UUID, Boolean> DEBUG = new HashMap<>();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AAMRWR");

    private RwrHealth() {}

    /** 取某个生物要显示的血量/护甲喵（会自动维护本地模型喵） */
    public static Display of(LivingEntity le) {
        float serverHp = le.getHealth();
        float serverMax = Math.max(1.0f, le.getMaxHealth());
        int armor = le.getArmorValue();

        Float vanillaBoxed = VANILLA_MAX.get(le.getType());
        float vanilla = vanillaBoxed != null ? vanillaBoxed : serverMax;

        // ① 最大血量明显不是原版 ② 活着的生物血量报 0 → 判定为「服务端隐藏/篡改血量」喵
        boolean maxAnomaly = vanillaBoxed != null
                && Math.abs(serverMax - vanilla) > Math.max(2.0f, vanilla * 0.25f);
        boolean hpHidden = serverHp <= 0.01f && le.isAlive();
        // 血量 > 最大血量（服务端把 health 塞成天文数字来盖住真实血量喵）也算异常喵
        boolean hpOverMax = serverHp > serverMax + 0.01f;

        if (maxAnomaly || hpHidden || hpOverMax) {
            if (DEBUG.containsKey(le.getUUID())) { /* 只打一次喵 */ } else {
                DEBUG.put(le.getUUID(), Boolean.TRUE);
                LOGGER.info("[RWR] 血量异常 {} type={} serverMax={} serverHp={} vanilla={} maxAnomaly={} hpHidden={}",
                        le.getName().getString(), le.getType(), serverMax, serverHp, vanilla, maxAnomaly, (hpHidden || hpOverMax));
            }
        }
        if (!maxAnomaly && !hpHidden && !hpOverMax) {
            MODELS.remove(le.getUUID());
            return new Display(serverHp, serverMax, armor, false);
        }

        Model m = MODELS.computeIfAbsent(le.getUUID(), k -> {
            Model n = new Model();
            n.max = vanilla;
            n.hp = vanilla;
            return n;
        });
        m.max = vanilla;

        if (maxAnomaly && !hpHidden && !hpOverMax && serverHp > 0.01f && serverMax > 0.01f) {
            // 最大血量被改，但比例仍可信 → 按比例换算成原版血量喵
            float ratio = Math.max(0.0f, Math.min(1.0f, serverHp / serverMax));
            m.hp = vanilla * ratio;
        } else {
            // 血量完全被隐藏（一直报 0 喵）→ 满血起步，按「受伤事件」本地扣血喵
            if (le.hurtTime > m.lastHurt) {
                m.hp -= RwrDamage.estimate(le);   // 用武器伤害 + 附魔估算喵
            }
            // 服务端偶尔漏出真实血量时校正一次喵
            if (serverHp > 0.01f && serverHp <= m.max + 0.01f && serverHp < m.hp) {
                m.hp = serverHp;
            }
        }
        m.lastHurt = le.hurtTime;
        m.hp = Math.max(0.0f, Math.min(m.max, m.hp));
        return new Display(m.hp, m.max, armor, true);
    }

    /** 清理已不存在的实体模型喵 */
    public static void prune() {
        if (MODELS.isEmpty()) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) {
            MODELS.clear();
            return;
        }
        MODELS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }
}
