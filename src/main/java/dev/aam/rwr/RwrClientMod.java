package dev.aam.rwr;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/** 纯客户端 RWR 入口喵 */
public final class RwrClientMod implements ClientModInitializer {

    public static KeyMapping toggleKey;
    public static KeyMapping lockKey;
    public static KeyMapping allKey;
    private static int beepCooldown = 0;

    @Override
    public void onInitializeClient() {
        RwrConfig.load();
        RwrWaypoints.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("aamrwr", "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aamrwr.toggle", GLFW.GLFW_KEY_H, category));
        lockKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aamrwr.lock", GLFW.GLFW_KEY_X, category));
        allKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aamrwr.all", GLFW.GLFW_KEY_Z, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 每 tick 更新锁定状态喵
            RwrLock.tick();
            RwrSpeed.tick();
            while (allKey.consumeClick()) {
                RwrConfig.allEntities = !RwrConfig.allEntities;
                RwrConfig.save();
                if (client.player != null) {
                    client.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            RwrConfig.allEntities ? "aamrwr.all_on" : "aamrwr.all_off"), true);
                }
            }
            while (lockKey.consumeClick()) {
                RwrLock.toggle();
            }
            while (toggleKey.consumeClick()) {
                RwrConfig.enabled = !RwrConfig.enabled;
                RwrConfig.save();
                if (client.player != null) {
                    client.player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    RwrConfig.enabled ? "aamrwr.on" : "aamrwr.off"), true);
                }
            }
        });
    }

    /** 每帧由 HUD 调用：告警音节奏喵 */
    public static void tickSound(boolean threat) {
        Minecraft mc = Minecraft.getInstance();
        if (!RwrConfig.sound || mc.getSoundManager() == null) {
            beepCooldown = 0;
            return;
        }
        if (!threat) {
            beepCooldown = 0;
            return;
        }
        if (beepCooldown <= 0) {
            beepCooldown = 6;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f, 0.6f));
        } else {
            beepCooldown--;
        }
    }
}
