package com.endersystem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Temporary testing commands until the K-key GUI arrives in Phase 3. */
public class SystemCommand {

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("system")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    SystemData d = p.getAttachedOrCreate(ModAttachments.SYSTEM);
                    String text = "=== SYSTEM ===\n"
                            + "Level: " + d.level() + "   Rank: " + d.rank() + "\n"
                            + "EXP: " + d.xp() + " / " + SystemData.xpForNextLevel(d.level()) + "\n"
                            + "Stat points: " + d.statPoints();
                    ctx.getSource().sendFeedback(() -> Text.literal(text), false);
                    return 1;
                })
                .then(CommandManager.literal("addxp")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            XpHandler.giveXp(p, IntegerArgumentType.getInteger(ctx, "amount"));
                            return 1;
                        })))
                .then(CommandManager.literal("reset")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        p.setAttached(ModAttachments.SYSTEM, SystemData.DEFAULT);
                        ctx.getSource().sendFeedback(() -> Text.literal("System reset."), false);
                        return 1;
                    }))));
    }
}
