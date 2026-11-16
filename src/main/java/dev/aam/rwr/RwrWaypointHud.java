package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** 航标点绘制喵：绿色点 + 名字 + 坐标 + 距离 */
public final class RwrWaypointHud {

    private static final int C_GREEN = 0xFF00FF66;

    private RwrWaypointHud() {}

    public static void render(GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui || !RwrConfig.enabled) return;
        if (RwrWaypoints.all().isEmpty()) return;

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
        Vec3 eye = mc.player.getEyePosition();

        for (RwrWaypoints.Point pt : RwrWaypoints.all().values()) {
            Vec3 d = new Vec3(pt.x() - eye.x, pt.y() - eye.y, pt.z() - eye.z);
            double z = d.dot(forward);
            if (z < 0.4) continue;              // 身后不画喵

            double sx = w / 2.0 + (d.dot(right) / z) / tanHalfH * (w / 2.0);
            double sy = h / 2.0 - (d.dot(up) / z) / tanHalfV * (h / 2.0);
            if (sx < 4 || sx > w - 4 || sy < 4 || sy > h - 4) continue;

            int px = (int) Math.round(sx);
            int py = (int) Math.round(sy);

            // 绿色方点（带黑边更好认喵）
            g.fill(px - 3, py - 3, px + 4, py + 4, 0xB0000000);
            g.fill(px - 2, py - 2, px + 3, py + 3, C_GREEN);

            String label = pt.name() + "  " + String.format("%.0f %.0f %.0f", pt.x(), pt.y(), pt.z());
            String dist = String.format("%.0fm", Math.sqrt(d.lengthSqr()));
            int tw = mc.font.width(label);
            int dw = mc.font.width(dist);

            g.drawString(mc.font, label, px - tw / 2, py + 7, C_GREEN, true);
            g.drawString(mc.font, dist, px - dw / 2, py + 18, 0xFFB0FFB0, true);
        }
    }
}
