package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** 纯客户端 RWR HUD：左下角雷达盘 + 光点 + 告警文字喵 */
public final class RwrHud {

    private static final int R_OUT = 44;
    private static final int R_CENTER = 8;
    private static final int MARGIN = 22;
    private static final double R_RING_OUT = R_OUT - 7.0;
    private static final double R_RING_MID = R_OUT * 0.72;
    private static final double R_RING_IN = R_OUT * 0.50;

    private static final int C_GREEN = 0xFF00FF66;
    private static final int C_GREEN_DIM = 0x3A00FF66;
    private static final int C_THREAT = 0xFFFF4040;
    private static final int C_BLUE = 0xFF40C0FF;
    private static final int C_HOSTILE = 0xFFFF3030;   // 敌对：红喵
    private static final int C_NEUTRAL = 0xFFFFD23B;   // 中立/友好：黄喵
    private static final int C_ITEM = 0xFFB060FF;      // 掉落物：紫喵

    private RwrHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui || !RwrConfig.enabled) return;

        RwrScanner.Scan scan = RwrScanner.scan();
        if (RwrConfig.requireHelmet && !scan.helmet()) return;

        int cx = MARGIN + R_OUT;
        int cy = g.guiHeight() - MARGIN - R_OUT;

        ringLine(g, cx, cy, R_RING_OUT + 3.0, C_GREEN);
        ringLine(g, cx, cy, R_RING_MID, C_GREEN_DIM);
        ringLine(g, cx, cy, R_RING_IN, C_GREEN_DIM);
        ringLine(g, cx, cy, R_CENTER, C_GREEN);
        g.fill(cx - 3, cy, cx + 4, cy + 1, C_GREEN);
        g.fill(cx, cy - 3, cx + 1, cy + 4, C_GREEN);

        for (RwrScanner.Contact c : scan.contacts()) {
            double br = c.dist() > 200.0 ? R_RING_OUT
                    : (c.dist() > 120.0 ? R_RING_MID : (c.dist() > 60.0 ? R_RING_IN : R_RING_IN * 0.7));

            switch (c.type()) {
                case RwrScanner.TYPE_PLAYER -> {
                    int color = c.friendly() ? C_BLUE : C_HOSTILE;
                    blipDot(g, cx, cy, br, (float) c.bearing(), color, false);
                }
                case RwrScanner.TYPE_PROJECTILE ->
                        blipArc(g, cx, cy, br, (float) c.bearing(), C_THREAT);
                case RwrScanner.TYPE_HOSTILE ->
                        blipDot(g, cx, cy, br, (float) c.bearing(), C_HOSTILE, true);   // 带边框喵
                case RwrScanner.TYPE_NEUTRAL ->
                        blipDot(g, cx, cy, br, (float) c.bearing(), C_NEUTRAL, false);
                case RwrScanner.TYPE_ITEM ->
                        blipDot(g, cx, cy, br, (float) c.bearing(), C_ITEM, false);
                default -> { }
            }
        }

        // 告警文字喵
        if (scan.threat()) {
            String label = "敌导弹";
            int tw = mc.font.width(label);
            int bw = tw + 10;
            int bh = 12;
            int bx = cx - bw / 2;
            int by = cy - R_OUT - bh - 6;
            g.fill(bx, by, bx + bw, by + 1, C_THREAT);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, C_THREAT);
            g.fill(bx, by, bx + 1, by + bh, C_THREAT);
            g.fill(bx + bw - 1, by, bx + bw, by + bh, C_THREAT);
            g.drawString(mc.font, label, cx - tw / 2, by + 2, C_THREAT, false);
        }
    }

    // ==================== 绘图喵 ====================

    /** 圆环（扫描线填充喵） */
    private static void ringLine(GuiGraphics g, int cx, int cy, double r, int color) {
        int R = (int) Math.ceil(r) + 1;
        for (int dy = -R; dy <= R; dy++) {
            double y = dy;
            double o2 = (r + 0.5) * (r + 0.5) - y * y;
            if (o2 <= 0) continue;
            int xo = (int) Math.floor(Math.sqrt(o2));
            double inner = Math.max(0.0, r - 0.5);
            int xi = 0;
            double i2 = inner * inner - y * y;
            if (i2 > 0) xi = (int) Math.ceil(Math.sqrt(i2));
            if (xi > 0) {
                g.fill(cx - xo, cy + dy, cx - xi + 1, cy + dy + 1, color);
                g.fill(cx + xi, cy + dy, cx + xo + 1, cy + dy + 1, color);
            } else {
                g.fill(cx - xo, cy + dy, cx + xo + 1, cy + dy + 1, color);
            }
        }
    }

    /** 环带弧段（导弹用喵） */
    private static void blipArc(GuiGraphics g, int cx, int cy, double r, float bearing, int color) {
        double halfSpan = 9.0;
        double rIn = r - 2.4;
        double rOut = r + 2.4;
        int R = (int) Math.ceil(rOut) + 1;
        for (int dy = -R; dy <= R; dy++) {
            double y = dy;
            double o2 = rOut * rOut - y * y;
            if (o2 <= 0) continue;
            int xo = (int) Math.floor(Math.sqrt(o2));
            int xi = 0;
            double i2 = rIn * rIn - y * y;
            if (i2 > 0) xi = (int) Math.ceil(Math.sqrt(i2));
            for (int dx = -xo; dx <= xo; dx++) {
                if (Math.abs(dx) < xi) continue;
                double ang = Math.toDegrees(Math.atan2(dx, -y));
                double diff = ((ang - bearing + 540.0) % 360.0) - 180.0;
                if (Math.abs(diff) > halfSpan) continue;
                g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }
    }

    /** 玩家小圆点喵 */
    private static void blipDot(GuiGraphics g, int cx, int cy, double r, float bearing, int color, boolean border) {
        double rad = Math.toRadians(bearing);
        int x = cx + (int) Math.round(Math.sin(rad) * r);
        int y = cy - (int) Math.round(Math.cos(rad) * r);
        if (border) {
            // 带边框的方块（敌对生物喵）：外圈深色 + 内圈亮色 + 中间空心感喵
            g.fill(x - 3, y - 3, x + 4, y + 4, 0xFF101010);
            g.fill(x - 2, y - 2, x + 3, y + 3, color);
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF101010);
        } else {
            g.fill(x - 2, y - 2, x + 3, y + 3, 0x90000000);
            g.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    /** 用于外部（可选）：视野内玩家头顶红/蓝点喵 */
    public static void renderPlayerDots(GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui || !RwrConfig.enabled) return;

        RwrScanner.Scan scan = RwrScanner.scan();
        if (RwrConfig.requireHelmet && !scan.helmet()) return;

        int w = g.guiWidth();
        int h = g.guiHeight();
        double fovV = mc.options.fov().get();
        double aspect = (double) w / Math.max(1, h);
        double fovH = 2.0 * Math.toDegrees(Math.atan(Math.tan(Math.toRadians(fovV) / 2.0) * aspect));
        double tanHalfV = Math.tan(Math.toRadians(fovV) / 2.0);
        double tanHalfH = Math.tan(Math.toRadians(fovH) / 2.0);

        var cam = mc.gameRenderer.getMainCamera();
        if (cam == null || !cam.isInitialized()) return;
        Vec3 forward = new Vec3(cam.forwardVector());
        Vec3 up = new Vec3(cam.upVector());
        Vec3 right = new Vec3(cam.leftVector()).scale(-1.0);
        for (RwrScanner.Contact c : scan.contacts()) {
            if (c.type() != RwrScanner.TYPE_PLAYER) continue;
            // 直接用「相对眼睛的世界偏移」投影，避免角度反推的二次误差喵
            Vec3 d = new Vec3(c.dx(), c.dy(), c.dz());
            double z = d.dot(forward);
            if (z < 0.5) continue;
            double sx = w / 2.0 + (d.dot(right) / z) / tanHalfH * (w / 2.0);
            double sy = h / 2.0 - (d.dot(up) / z) / tanHalfV * (h / 2.0);
            if (sx < 2 || sx > w - 2 || sy < 2 || sy > h - 2) continue;
            int px = (int) Math.round(sx);
            int py = (int) Math.round(sy) - 14;
            int color = c.friendly() ? C_BLUE : C_THREAT;
            g.fill(px - 2, py - 2, px + 3, py + 3, 0x90000000);
            g.fill(px - 1, py - 1, px + 2, py + 2, color);
        }
    }
}
