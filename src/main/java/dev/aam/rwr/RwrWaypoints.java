package dev.aam.rwr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 航标点（纯客户端，存在 config/aam-rwr-waypoints.json 喵）
 *  - /rwrset name x y z 添加
 *  - /rwrremove name 删除
 *  - 走到 30 格内自动删除喵
 */
public final class RwrWaypoints {

    public record Point(String name, double x, double y, double z) {}

    /** 自动删除距离（格喵） */
    public static final double AUTO_REMOVE_DIST = 30.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Point> POINTS = new LinkedHashMap<>();

    private RwrWaypoints() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aam-rwr-waypoints.json");
    }

    public static Map<String, Point> all() {
        return POINTS;
    }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            Map<String, Point> loaded = GSON.fromJson(r, new TypeToken<LinkedHashMap<String, Point>>() {}.getType());
            if (loaded != null) {
                POINTS.clear();
                POINTS.putAll(loaded);
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(POINTS, w);
        } catch (IOException ignored) {
        }
    }

    public static void set(String name, double x, double y, double z) {
        POINTS.put(name, new Point(name, x, y, z));
        save();
    }

    public static boolean remove(String name) {
        boolean removed = POINTS.remove(name) != null;
        if (removed) save();
        return removed;
    }

    /** 每帧检查：走到 30 格内自动删除喵 */
    public static void tickAutoRemove(net.minecraft.world.entity.player.Player player) {
        if (POINTS.isEmpty()) return;
        boolean changed = false;
        var it = POINTS.entrySet().iterator();
        while (it.hasNext()) {
            Point pt = it.next().getValue();
            double dx = player.getX() - pt.x();
            double dy = player.getY() - pt.y();
            double dz = player.getZ() - pt.z();
            if (dx * dx + dy * dy + dz * dz <= AUTO_REMOVE_DIST * AUTO_REMOVE_DIST) {
                it.remove();
                changed = true;
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("aamrwr.waypoint_reached", pt.name()), true);
            }
        }
        if (changed) save();
    }
}
