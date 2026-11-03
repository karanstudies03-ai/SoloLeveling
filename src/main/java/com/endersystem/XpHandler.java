package com.endersystem;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class XpHandler {

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
            int xp = xpFor(entity);
            if (xp > 0) {
                int gold = Math.max(1, xp / 5);
                giveXp(player, xp, gold);
            }
        });
    }

    public static int xpFor(LivingEntity entity) {
        if (entity instanceof EnderDragonEntity) return 5000;
        if (entity instanceof DesertCentipedeEntity) return 300; // B-Rank
        if (entity instanceof HostileEntity) {
            return Math.max(5, (int) (entity.getMaxHealth() * 2));
        }
        return 0;
    }

    public static void giveXp(ServerPlayerEntity player, int amount) {
        giveXp(player, amount, 0);
    }

    public static void giveXp(ServerPlayerEntity player, int amount, int gold) {
        SystemData before = player.getAttachedOrCreate(ModAttachments.SYSTEM);
        SystemData after = before.addXp(amount).addGold(gold);
        player.setAttached(ModAttachments.SYSTEM, after);

        String msg = "[SYSTEM] +" + amount + " XP" + (gold > 0 ? "   +" + gold + " gold" : "");
        player.sendMessage(Text.literal(msg).formatted(Formatting.AQUA), true);

        if (after.level() > before.level()) {
            ModPackets.notify(player, "LEVEL UP!",
                    "Level " + after.level() + "  |  Rank " + after.rank(),
                    "+" + (after.statPoints() - before.statPoints()) + " stat points");
        }
    }
}
