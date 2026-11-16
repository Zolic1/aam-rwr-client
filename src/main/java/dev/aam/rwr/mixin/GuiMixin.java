package dev.aam.rwr.mixin;

import dev.aam.rwr.RwrClientMod;
import dev.aam.rwr.RwrHud;
import dev.aam.rwr.RwrScanner;
import dev.aam.rwr.RwrWaypoints;
import dev.aam.rwr.RwrLockHud;
import dev.aam.rwr.RwrWaypointHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 HUD 最后绘制 RWR 喵 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void aamrwr$render(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        RwrWaypoints.tickAutoRemove(mc.player);
        RwrHud.render(g);
        RwrWaypointHud.render(g, mc);
        RwrLockHud.render(g, mc);
        RwrHud.renderPlayerDots(g, mc);
        RwrClientMod.tickSound(RwrScanner.scan().threat());
    }
}
