package com.endersystem;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Penalty Quest. If the daily quest is failed, the player is teleported to the Penalty Zone
 * (a huge desert, see Arena.java) and must survive 2 minutes against 3 powerful
 * Poison-Toothed Giant Desert Centipedes. Surviving gives a reward and sends the player back.
 * Dying ends the penalty.
 */
public class Penalty {
    public static final String TAG = "endersystem_penalty";

    private static final int CENTIPEDE_COUNT = 3;
    private static final int STRENGTH_AMPLIFIER = 4; // Strength V
    private static final int REWARD_POINTS = 3;

    private static final Map<UUID, Integer> LEFT = new HashMap<>();
    private static final Random RNG = new Random();
    private static MinecraftServer serverRef;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> serverRef = s);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                tick(server, p);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LEFT.remove(handler.player.getUuid()));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) onPlayerDeath(p);
        });

        // Centipede bite = poison
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (!blocked && source.getAttacker() instanceof DesertCentipedeEntity
                    && entity instanceof ServerPlayerEntity victim) {
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 120, 0));
            }
        });

        // No natural monsters (creepers!) in the Penalty Zone: they would blow up the ground.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof HostileEntity && !(entity instanceof DesertCentipedeEntity)
                    && world.getRegistryKey().equals(World.OVERWORLD)
                    && Arena.contains(entity.getX(), entity.getZ())) {
                world.getServer().execute(entity::discard);
            }
        });
    }

    // ------------------------------------------------------------------ start / end

    /** Starts the penalty. Returns false if it could not start. */
    public static boolean start(MinecraftServer server, ServerPlayerEntity p, boolean force) {
        if (!p.isAlive()) return false;
        if (!force && (p.isCreative() || p.isSpectator())) return false;
        PenaltyData current = p.getAttachedOrCreate(ModAttachments.PENALTY);
        if (current.active()) return false;

        ServerWorld arena = server.getOverworld();
        // Make sure there is ground under the landing spot (a few chunks around the centre)
        Arena.ensureChunks(arena, Arena.CENTER_X >> 4, Arena.CENTER_Z >> 4, 12, Integer.MAX_VALUE);

        String dim = p.getEntityWorld().getRegistryKey().getValue().toString();
        p.setAttached(ModAttachments.PENALTY, new PenaltyData(true, PenaltyData.DURATION_TICKS, dim,
                p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch()));
        LEFT.put(p.getUuid(), PenaltyData.DURATION_TICKS);

        double landY = Arena.surfaceY(Arena.CENTER_X, Arena.CENTER_Z) + 1;
        p.teleport(arena, Arena.CENTER_X + 0.5, landY, Arena.CENTER_Z + 0.5, Set.of(), 0f, 0f, true);
        ModPackets.notify(p, "PENALTY QUEST", "Daily Quest failed!", "Survive 2 minutes");
        return true;
    }

    /** Ends the penalty and sends the player back. */
    public static void end(MinecraftServer server, ServerPlayerEntity p, boolean success) {
        PenaltyData pd = p.getAttachedOrCreate(ModAttachments.PENALTY);
        if (!pd.active()) return;

        LEFT.remove(p.getUuid());
        p.setAttached(ModAttachments.PENALTY, PenaltyData.NONE);

        ServerWorld back = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(pd.dim())));
        if (back == null) back = server.getOverworld();
        p.teleport(back, pd.rx(), pd.ry(), pd.rz(), Set.of(), pd.yaw(), pd.pitch(), true);
        p.removeStatusEffect(StatusEffects.POISON);

        cleanupIfNobodyInside(server);

        if (success) {
            int gold = 100 + RNG.nextInt(401);
            SystemData d = p.getAttachedOrCreate(ModAttachments.SYSTEM);
            p.setAttached(ModAttachments.SYSTEM, d.addPoints(REWARD_POINTS).addGold(gold));
            ModPackets.notify(p, "PENALTY CLEARED", "You survived!",
                    "+" + REWARD_POINTS + " stat points, +" + gold + " gold");
        } else {
            p.sendMessage(Text.literal("[SYSTEM] Penalty Quest ended.").formatted(Formatting.RED), false);
        }
    }

    private static void onPlayerDeath(ServerPlayerEntity p) {
        PenaltyData pd = p.getAttachedOrCreate(ModAttachments.PENALTY);
        if (!pd.active()) return;
        LEFT.remove(p.getUuid());
        p.setAttached(ModAttachments.PENALTY, PenaltyData.NONE);
        if (serverRef != null) cleanupIfNobodyInside(serverRef);
        p.sendMessage(Text.literal("[SYSTEM] You died in the Penalty Zone. Penalty failed.")
                .formatted(Formatting.RED), false);
    }

    // ------------------------------------------------------------------ per tick

    private static void tick(MinecraftServer server, ServerPlayerEntity p) {
        if (!p.isAlive()) return;
        PenaltyData pd = p.getAttachedOrCreate(ModAttachments.PENALTY);
        if (!pd.active()) return;

        UUID id = p.getUuid();
        int left = LEFT.getOrDefault(id, pd.ticksLeft()) - 1;
        LEFT.put(id, left);
        int elapsed = PenaltyData.DURATION_TICKS - left;

        ServerWorld arena = server.getOverworld();

        // Keep generating desert ahead of the player (max 2 chunks per tick, no lag spikes)
        if (Arena.contains(p.getX(), p.getZ())) {
            Arena.ensureChunks(arena, p.getBlockX() >> 4, p.getBlockZ() >> 4, 25, 2);
        }

        // Exactly 3 centipedes, spawned once, 1 second after arriving
        if (elapsed == 20) {
            spawnCentipedes(arena, p, CENTIPEDE_COUNT);
        }

        if (left <= 0) {
            end(server, p, true);
            return;
        }

        if (left % 20 == 0) {
            p.setAttached(ModAttachments.PENALTY, pd.withTicks(left)); // synced once per second
        }
    }

    // ------------------------------------------------------------------ mobs

    private static void spawnCentipedes(ServerWorld world, ServerPlayerEntity p, int count) {
        for (int i = 0; i < count; i++) {
            DesertCentipedeEntity c = ModEntities.DESERT_CENTIPEDE.create(world, SpawnReason.EVENT);
            if (c == null) continue;

            // spread around the player, 25-35 blocks away
            double angle = (Math.PI * 2 / count) * i + RNG.nextDouble() * 0.6;
            double dist = 25 + RNG.nextDouble() * 10;
            int x = (int) Math.floor(p.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(p.getZ() + Math.sin(angle) * dist);
            int y = Arena.surfaceY(x, z) + 1;

            c.refreshPositionAndAngles(x + 0.5, y, z + 0.5, RNG.nextFloat() * 360f, 0f);
            c.setPersistent();
            c.addCommandTag(TAG);
            c.setCustomName(Text.literal("[B-Rank] Poison-Toothed Giant Desert Centipede")
                    .formatted(Formatting.RED));
            c.setCustomNameVisible(true);
            // Strength V, permanent, no particles
            c.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                    StatusEffectInstance.INFINITE, STRENGTH_AMPLIFIER, false, false));
            world.spawnEntity(c);
        }
    }

    private static void cleanupIfNobodyInside(MinecraftServer server) {
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other.getAttachedOrCreate(ModAttachments.PENALTY).active()) return;
        }
        ServerWorld arena = server.getOverworld();
        List<DesertCentipedeEntity> mobs = arena.getEntitiesByClass(DesertCentipedeEntity.class, Arena.box(),
                e -> e.getCommandTags().contains(TAG));
        for (DesertCentipedeEntity m : mobs) m.discard();
    }
}
