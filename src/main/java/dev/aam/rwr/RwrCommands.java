package dev.aam.rwr;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

/**
 * 客户端指令喵（不做任何权限检查，所有人都能用喵）
 *   /rwrset <名字> <x> <y> <z>
 *   /rwrremove <名字>
 */
public final class RwrCommands implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("rwrset")
                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                            .then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                    .then(ClientCommandManager.argument("y", DoubleArgumentType.doubleArg(-2048, 2048))
                                            .then(ClientCommandManager.argument("z", DoubleArgumentType.doubleArg(-30000000, 30000000))
                                                    .executes(ctx -> {
                                                        String name = StringArgumentType.getString(ctx, "name");
                                                        double x = DoubleArgumentType.getDouble(ctx, "x");
                                                        double y = DoubleArgumentType.getDouble(ctx, "y");
                                                        double z = DoubleArgumentType.getDouble(ctx, "z");
                                                        RwrWaypoints.set(name, x, y, z);
                                                        ctx.getSource().sendFeedback(Component.translatable(
                                                                "aamrwr.waypoint_added", name,
                                                                String.format("%.1f %.1f %.1f", x, y, z)));
                                                        return 1;
                                                    }))))));

            dispatcher.register(ClientCommandManager.literal("rwrremove")
                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                if (RwrWaypoints.remove(name)) {
                                    ctx.getSource().sendFeedback(Component.translatable("aamrwr.waypoint_removed", name));
                                    return 1;
                                }
                                ctx.getSource().sendError(Component.translatable("aamrwr.waypoint_missing", name));
                                return 0;
                            })));
        });
    }
}
