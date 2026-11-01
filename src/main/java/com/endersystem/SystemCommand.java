package com.endersystem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Admin/testing commands only. Players use the K key. */
public class SystemCommand {

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("system")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(CommandManager.literal("addxp")
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            XpHandler.giveXp(p, IntegerArgumentType.getInteger(ctx, "amount"));
                            return 1;
                        })))
                .then(CommandManager.literal("reset")
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        p.setAttached(ModAttachments.SYSTEM, SystemData.DEFAULT);
                        StatEffects.apply(p);
                        ctx.getSource().sendFeedback(() -> Text.literal("System reset."), false);
                        return 1;
                    }))));
    }
}
