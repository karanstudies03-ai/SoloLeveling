package com.endersystem;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
            if (xp > 0) giveXp(player, xp);
        });
    }

    public static int xpFor(LivingEntity entity) {
        if (entity instanceof EnderDragonEntity) return 5000;
        if (entity instanceof HostileEntity) {
            return Math.max(5, (int) (entity.getMaxHealth() * 2));
        }
        return 0;
    }

    public static void giveXp(ServerPlayerEntity player, int amount) {
        SystemData before = player.getAttachedOrCreate(ModAttachments.SYSTEM);
        SystemData after = before.addXp(amount);
        player.setAttached(ModAttachments.SYSTEM, after);

        player.sendMessage(Text.literal("[SYSTEM] +" + amount + " XP")
                .formatted(Formatting.AQUA), true);

        if (after.level() > before.level()) {
            ServerPlayNetworking.send(player, new ModPackets.LevelUpPayload(
                    after.level(), after.statPoints() - before.statPoints()));
        }
    }
}
