package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * 客户端速度估算喵：
 *  - 服务端对「其他玩家/远处实体」不一定会下发速度，getDeltaMovement() 常常是 0喵
 *  - 所以这里用「最近 3 秒的客户端位移采样」算平均速度（路径长度 / 时间喵）
 */
public final class RwrSpeed {

    /** 采样窗口（tick 喵，60 = 3 秒喵） */
    public static final int WINDOW_TICKS = 60;

    private record Sample(long tick, Vec3 pos) {}

    private static UUID targetId;
    private static final Deque<Sample> SAMPLES = new ArrayDeque<>();
    private static long tickCounter = 0;

    private RwrSpeed() {}

    /** 每客户端 tick 调用喵 */
    public static void tick() {
        tickCounter++;
        Entity target = RwrLock.locked();
        if (target == null) {
            targetId = null;
            SAMPLES.clear();
            return;
        }
        if (!target.getUUID().equals(targetId)) {
            targetId = target.getUUID();
            SAMPLES.clear();
        }
        SAMPLES.addLast(new Sample(tickCounter, target.position()));
        while (!SAMPLES.isEmpty() && tickCounter - SAMPLES.peekFirst().tick() > WINDOW_TICKS) {
            SAMPLES.removeFirst();
        }
    }

    /** 当前目标的速度（格/秒喵）：优先用 3 秒平均，采样不足时回退到 deltaMovement喵 */
    public static double speedOf(Entity e) {
        if (e == null) return 0.0;
        if (targetId != null && e.getUUID().equals(targetId) && SAMPLES.size() >= 2) {
            double path = 0.0;
            Vec3 prev = null;
            long firstTick = 0;
            long lastTick = 0;
            for (Sample s : SAMPLES) {
                if (prev == null) {
                    firstTick = s.tick();
                } else {
                    path += prev.distanceTo(s.pos());
                }
                prev = s.pos();
                lastTick = s.tick();
            }
            double seconds = (lastTick - firstTick) / 20.0;
            if (seconds > 0.05) {
                return path / seconds;
            }
        }
        // 回退：用实体自带速度（自己/近距离实体通常有效喵）
        return e.getDeltaMovement().length() * 20.0;
    }

    /** 是否正在使用 3 秒平均（HUD 可据此标记 "~" 喵） */
    public static boolean usingAverage(Entity e) {
        return e != null && targetId != null && e.getUUID().equals(targetId) && SAMPLES.size() >= 2;
    }

    /** 采样窗口内已经积累了多少秒喵 */
    public static double sampledSeconds() {
        if (SAMPLES.size() < 2) return 0.0;
        return (SAMPLES.peekLast().tick() - SAMPLES.peekFirst().tick()) / 20.0;
    }

    /** 清空采样（换目标时用喵） */
    public static void reset() {
        targetId = null;
        SAMPLES.clear();
    }

    /** 调试用：当前客户端 tick 喵 */
    public static long now() {
        return tickCounter;
    }

    static {
        // 保证 Minecraft 引用被用上（避免某些编译器警告喵）
        Minecraft.getInstance();
    }
}
