package dev.aam.rwr;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 纯客户端 RWR 配置喵（存在 config/aam-rwr-client.properties）
 */
public final class RwrConfig {

    /** 探测半径（格喵） */
    public static double range = 128.0;
    /** 是否只在戴 RWR 头盔时显示喵 */
    public static boolean requireHelmet = false;
    /** 是否播放告警音喵 */
    public static boolean sound = true;
    /** 导弹接近多少格内开始告警（0 = 只要朝我飞来就告警喵） */
    public static double threatRange = 0.0;
    /** HUD 总开关喵（也可用 H 键切换喵） */
    public static boolean enabled = true;
    /** 全实体模式喵（Z 键切换：敌对/中立/物品都显示喵） */
    public static boolean allEntities = false;
    /** 血量被隐藏时，每次受伤事件估算扣多少血喵（默认 5.0 喵） */
    public static double hiddenHitDamage = 5.0;
    /** 目标为玩家时假定的保护附魔减伤（0.64 ≈ 满身保护 IV 喵） */
    public static double playerProtection = 0.64;
    /** 读不到护甲时的默认值喵（玩家 20 / 怪物 1 喵） */
    public static double defaultPlayerArmor = 20.0;
    public static double defaultMobArmor = 1.0;
    /** 读不到附魔时的默认减伤喵（玩家 保护IV=0.64 / 怪物 0 喵） */
    public static double defaultPlayerProtection = 0.64;
    public static double defaultMobProtection = 0.0;

    private RwrConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aam-rwr-client.properties");
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) {
            save();
            return;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            props.load(reader);
        } catch (IOException e) {
            return;
        }
        range = clamp(read(props, "range", range), 16.0, 512.0);
        requireHelmet = Boolean.parseBoolean(props.getProperty("require_helmet", String.valueOf(requireHelmet)));
        sound = Boolean.parseBoolean(props.getProperty("sound", String.valueOf(sound)));
        threatRange = clamp(read(props, "threat_range", threatRange), 0.0, 256.0);
        enabled = Boolean.parseBoolean(props.getProperty("enabled", String.valueOf(enabled)));
        allEntities = Boolean.parseBoolean(props.getProperty("all_entities", String.valueOf(allEntities)));
        hiddenHitDamage = clamp(read(props, "hidden_hit_damage", hiddenHitDamage), 0.5, 40.0);
        playerProtection = clamp(read(props, "player_protection", playerProtection), 0.0, 0.95);
        defaultPlayerArmor = clamp(read(props, "default_player_armor", defaultPlayerArmor), 0.0, 40.0);
        defaultMobArmor = clamp(read(props, "default_mob_armor", defaultMobArmor), 0.0, 40.0);
        defaultPlayerProtection = clamp(read(props, "default_player_protection", defaultPlayerProtection), 0.0, 0.95);
        defaultMobProtection = clamp(read(props, "default_mob_protection", defaultMobProtection), 0.0, 0.95);
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("range", String.valueOf(range));
        props.setProperty("require_helmet", String.valueOf(requireHelmet));
        props.setProperty("sound", String.valueOf(sound));
        props.setProperty("threat_range", String.valueOf(threatRange));
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("all_entities", String.valueOf(allEntities));
        props.setProperty("hidden_hit_damage", String.valueOf(hiddenHitDamage));
        props.setProperty("player_protection", String.valueOf(playerProtection));
        props.setProperty("default_player_armor", String.valueOf(defaultPlayerArmor));
        props.setProperty("default_mob_armor", String.valueOf(defaultMobArmor));
        props.setProperty("default_player_protection", String.valueOf(defaultPlayerProtection));
        props.setProperty("default_mob_protection", String.valueOf(defaultMobProtection));
        try (Writer writer = Files.newBufferedWriter(file())) {
            props.store(writer, "AAM RWR (client-side) config");
        } catch (IOException ignored) {
        }
    }

    private static double read(Properties props, String key, double def) {
        try {
            return Double.parseDouble(props.getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
