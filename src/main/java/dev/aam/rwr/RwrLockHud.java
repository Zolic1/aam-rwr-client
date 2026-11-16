package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 纯客户端锁定框喵（修改版喵）：
 *  - 用「相机位置 + 相机基向量」投影，和原版渲染完全同一套口径（修正第三人称/视角偏移喵）
 *  - 目标位置用插值（partialTick），不会和渲染出来的实体错位喵
 *  - 目标在身后/出屏 → 屏幕边缘箭头指示喵
 */
public final class RwrLockHud {

    private static final int C_GREEN = 0xFF00FF66;
    private static final int C_LOCKING = 0xCC00FF66;
    private static final int C_SOFT = 0xAA00FF66;
    private static final int C_EDGE = 0xFF00FF66;

    private RwrLockHud() {}

    public static void render(GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui || !RwrConfig.enabled) return;

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Entity target = RwrLock.locked();

        if (target == null) {
            float p = RwrLock.progress();
            if (p > 0.0f) {
                var text = net.minecraft.network.chat.Component.translatable(
                        "aamrwr.locking", Math.round(p * 100));
                int w = mc.font.width(text);
                g.drawString(mc.font, text, g.guiWidth() / 2 - w / 2, g.guiHeight() / 2 + 46, C_LOCKING, true);
            }
            return;
        }

        View v = view(mc);
        if (v == null) return;

        Vec3 center = target.getPosition(partial).add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 d = center.subtract(v.eye);
        double z = d.dot(v.forward);
        double px = d.dot(v.right);
        double py = d.dot(v.up);

        // 视野外 / 身后 → 边缘箭头喵
        if (z < 0.4 || Math.abs(px / z) > v.tanHalfH * 0.95 || Math.abs(py / z) > v.tanHalfV * 0.95) {
            drawEdgeArrow(g, mc, v, px, py, z, target);
            return;
        }

        double sx = v.w / 2.0 + (px / z) / v.tanHalfH * (v.w / 2.0);
        double sy = v.h / 2.0 - (py / z) / v.tanHalfV * (v.h / 2.0);
        int x = (int) Math.round(sx);
        int y = (int) Math.round(sy);

        // 框大小按「目标宽度 / 距离」缩放，近大远小不跳动喵
        double screenSize = (Math.max(target.getBbWidth(), 0.4) * v.h * 0.5 / Math.max(0.5, z) / v.tanHalfV);
        int hh = (int) Math.max(10, Math.min(80, screenSize));
        int hw = (int) Math.max(10, Math.min(80, screenSize));
        int leg = Math.max(4, Math.min(hw, hh) / 3);

        g.fill(x - hw, y - hh, x - hw + leg, y - hh + 1, C_GREEN);
        g.fill(x - hw, y - hh, x - hw + 1, y - hh + leg, C_GREEN);
        g.fill(x + hw - leg, y - hh, x + hw, y - hh + 1, C_GREEN);
        g.fill(x + hw - 1, y - hh, x + hw, y - hh + leg, C_GREEN);
        g.fill(x - hw, y + hh - 1, x - hw + leg, y + hh, C_GREEN);
        g.fill(x - hw, y + hh - leg, x - hw + 1, y + hh, C_GREEN);
        g.fill(x + hw - leg, y + hh - 1, x + hw, y + hh, C_GREEN);
        g.fill(x + hw - 1, y + hh - leg, x + hw, y + hh, C_GREEN);

        String name = target.getName().getString();
        int nw = mc.font.width(name);
        g.drawString(mc.font, name, x - nw / 2, y - hh - 12, C_GREEN, true);

        int barW = Math.max(nw, hw * 2);
        int barX = x - barW / 2;
        int barY = y + hh + 4;

        int extraLines = 0;
        if (target instanceof LivingEntity le) {
            RwrHealth.Display hd = RwrHealth.of(le);
            float ratio = Math.max(0.0f, Math.min(1.0f, hd.hp() / hd.max()));
            g.fill(barX - 1, barY - 1, barX + barW + 1, barY + 5, 0xB0000000);
            g.fill(barX, barY, barX + barW, barY + 4, 0xFF203020);
            int color = ratio > 0.5f ? C_GREEN : (ratio > 0.25f ? 0xFFFFD23B : 0xFFFF3030);
            g.fill(barX, barY, barX + (int) (barW * ratio), barY + 4, color);

            String hpText = (int) Math.ceil(hd.hp()) + " / " + (int) Math.ceil(hd.max());
            if (hd.localModel()) hpText = "~" + hpText;      // 本地模型标记喵
            g.drawString(mc.font, hpText, x - mc.font.width(hpText) / 2, barY + 7, C_SOFT, true);
            extraLines++;

            // 护甲条喵（浅蓝，20 点满喵）
            if (hd.armor() > 0) {
                int armorY = barY + 18;
                float ar = Math.min(1.0f, hd.armor() / 20.0f);
                g.fill(barX - 1, armorY - 1, barX + barW + 1, armorY + 4, 0xB0000000);
                g.fill(barX, armorY, barX + barW, armorY + 3, 0xFF14202C);
                g.fill(barX, armorY, barX + (int) (barW * ar), armorY + 3, 0xFF7FB8FF);
                String armorText = net.minecraft.network.chat.Component
                        .translatable("aamrwr.armor", hd.armor()).getString();
                g.drawString(mc.font, armorText, x - mc.font.width(armorText) / 2, armorY + 4, 0xFF9CCBFF, true);
                extraLines++;
            }
        }

        double dist = v.eye.distanceTo(center);
        double spd = RwrSpeed.speedOf(target);
        String spdText = RwrSpeed.usingAverage(target)
                ? String.format("~%.1f m/s", spd)      // 约值（3 秒平均喵）
                : String.format("%.1f m/s", spd);
        String info = String.format("%.0fm   %s", dist, spdText);
        g.drawString(mc.font, info, x - mc.font.width(info) / 2,
                barY + 7 + extraLines * 11, C_GREEN, true);
    }

    /** 屏幕边缘箭头喵 */
    private static void drawEdgeArrow(GuiGraphics g, Minecraft mc, View v, double px, double py, double z,
                                      Entity target) {
        double dirX = px;
        double dirY = -py;
        if (z < 0.0) {          // 身后 → 反向指示喵
            dirX = -dirX;
            dirY = -dirY;
        }
        double norm = Math.sqrt(dirX * dirX + dirY * dirY);
        if (norm < 1.0E-4) {
            dirX = 0;
            dirY = 1;
            norm = 1;
        }
        dirX /= norm;
        dirY /= norm;

        double radius = Math.min(v.w, v.h) * 0.36;
        int ax = (int) Math.round(v.w / 2.0 + dirX * radius);
        int ay = (int) Math.round(v.h / 2.0 + dirY * radius);
        ax = Math.max(8, Math.min((int) v.w - 8, ax));
        ay = Math.max(8, Math.min((int) v.h - 8, ay));

        // 箭头三角喵
        int size = 5;
        int tx = (int) Math.round(ax + dirX * size);
        int ty = (int) Math.round(ay + dirY * size);
        int bx = (int) Math.round(ax - dirY * size);
        int by = (int) Math.round(ay + dirX * size);
        int cx = (int) Math.round(ax + dirY * size);
        int cy = (int) Math.round(ay - dirX * size);
        triangle(g, tx, ty, bx, by, cx, cy, C_EDGE);

        String name = target.getName().getString();
        double dist = v.eye.distanceTo(target.getBoundingBox().getCenter());
        String info = String.format("%.0fm", dist);
        int nw = mc.font.width(name);
        int iw = mc.font.width(info);
        boolean below = dirY > 0;
        g.drawString(mc.font, info, ax - iw / 2, below ? ay + 10 : ay - 20, C_GREEN, true);
        g.drawString(mc.font, name, ax - nw / 2, below ? ay + 20 : ay - 30, C_SOFT, true);
    }

    private static void triangle(GuiGraphics g, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double d1 = sign(x, y, x1, y1, x2, y2);
                double d2 = sign(x, y, x2, y2, x3, y3);
                double d3 = sign(x, y, x3, y3, x1, y1);
                boolean neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
                boolean pos = (d1 > 0) || (d2 > 0) || (d3 > 0);
                if (!(neg && pos)) {
                    g.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static double sign(int px, int py, int ax, int ay, int bx, int by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }

    /** 当前帧的相机参数喵 */
    private record View(double w, double h, Vec3 eye, Vec3 forward, Vec3 up, Vec3 right,
                        double tanHalfH, double tanHalfV) {}

    private static View view(Minecraft mc) {
        var cam = mc.gameRenderer.getMainCamera();
        if (cam == null || !cam.isInitialized()) return null;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        double fovV = mc.options.fov().get();
        double aspect = (double) w / Math.max(1, h);
        double fovH = 2.0 * Math.toDegrees(Math.atan(Math.tan(Math.toRadians(fovV) / 2.0) * aspect));
        return new View(w, h,
                cam.position(),                                  // 相机位置（不玩家眼睛喵）
                new Vec3(cam.forwardVector()),
                new Vec3(cam.upVector()),
                new Vec3(cam.leftVector()).scale(-1.0),
                Math.tan(Math.toRadians(fovH) / 2.0),
                Math.tan(Math.toRadians(fovV) / 2.0));
    }
}
