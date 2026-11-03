package com.endersystem;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Penalty Quest. If the daily quest is failed, the player is teleported to a sand arena
 * floating high above the Overworld and must survive 2 minutes against Poison-Toothed Giant
 * Desert Centipedes. Surviving gives a reward and sends the player back. Dying ends the penalty.
 */
public class Penalty {
    public static final String TAG = "endersystem_penalty";

    // Arena: a round sand platform, far away and high up, ringed by invisible barrier walls.
    private static final int ARENA_X = 30000;
    private static final int ARENA_Y = 290;
    private static final int ARENA_Z = 30000;
    private static final int RADIUS = 30;

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
    }

    // ------------------------------------------------------------------ start / end

    /** Starts the penalty. Returns false if it could not start. */
    public static boolean start(MinecraftServer server, ServerPlayerEntity p, boolean force) {
        if (!p.isAlive()) return false;
        if (!force && (p.isCreative() || p.isSpectator())) return false;
        PenaltyData current = p.getAttachedOrCreate(ModAttachments.PENALTY);
        if (current.active()) return false;

        ServerWorld arena = server.getOverworld();
        buildArena(arena);

        String dim = p.getEntityWorld().getRegistryKey().getValue().toString();
        p.setAttached(ModAttachments.PENALTY, new PenaltyData(true, PenaltyData.DURATION_TICKS, dim,
                p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch()));
        LEFT.put(p.getUuid(), PenaltyData.DURATION_TICKS);

        p.teleport(arena, ARENA_X + 0.5, ARENA_Y + 2, ARENA_Z + 0.5, Set.of(), 0f, 0f, true);
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

        // Keep a few centipedes alive: first wave after 1 second, then top up every 5 seconds
        if (elapsed == 20 || left % 100 == 0) {
            int target = Math.min(8, 4 + elapsed / 400);
            int alive = countCentipedes(arena);
            int toSpawn = Math.min(Math.max(0, target - alive), elapsed == 20 ? 4 : 2);
            spawnCentipedes(arena, p, toSpawn);
        }

        if (left <= 0) {
            end(server, p, true);
            return;
        }

        if (left % 20 == 0) {
            p.setAttached(ModAttachments.PENALTY, pd.withTicks(left)); // synced once per second
        }
    }

    // ------------------------------------------------------------------ arena + mobs

    private static Box arenaBox() {
        int r = RADIUS + 6;
        return new Box(ARENA_X - r, ARENA_Y - 5, ARENA_Z - r, ARENA_X + r, ARENA_Y + 40, ARENA_Z + r);
    }

    private static int countCentipedes(ServerWorld world) {
        return world.getEntitiesByClass(DesertCentipedeEntity.class, arenaBox(),
                e -> e.isAlive() && e.getCommandTags().contains(TAG)).size();
    }

    private static void spawnCentipedes(ServerWorld world, ServerPlayerEntity p, int count) {
        for (int i = 0; i < count; i++) {
            DesertCentipedeEntity c = ModEntities.DESERT_CENTIPEDE.create(world, SpawnReason.EVENT);
            if (c == null) continue;

            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = 10 + RNG.nextDouble() * 8;
            double x = p.getX() + Math.cos(angle) * dist;
            double z = p.getZ() + Math.sin(angle) * dist;

            // keep inside the arena
            double dx = x - ARENA_X, dz = z - ARENA_Z;
            double len = Math.sqrt(dx * dx + dz * dz);
            double maxLen = RADIUS - 4;
            if (len > maxLen) {
                x = ARENA_X + dx / len * maxLen;
                z = ARENA_Z + dz / len * maxLen;
            }

            c.refreshPositionAndAngles(x, ARENA_Y + 2, z, RNG.nextFloat() * 360f, 0f);
            c.setPersistent();
            c.addCommandTag(TAG);
            c.setCustomName(Text.literal("[B-Rank] Poison-Toothed Giant Desert Centipede")
                    .formatted(Formatting.RED));
            c.setCustomNameVisible(true);
            world.spawnEntity(c);
        }
    }

    private static void cleanupIfNobodyInside(MinecraftServer server) {
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other.getAttachedOrCreate(ModAttachments.PENALTY).active()) return;
        }
        ServerWorld arena = server.getOverworld();
        List<DesertCentipedeEntity> mobs = arena.getEntitiesByClass(DesertCentipedeEntity.class, arenaBox(),
                e -> e.getCommandTags().contains(TAG));
        for (DesertCentipedeEntity m : mobs) m.discard();
    }

    private static void buildArena(ServerWorld w) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        if (w.getBlockState(pos.set(ARENA_X, ARENA_Y, ARENA_Z)).isOf(Blocks.SANDSTONE)) return; // already built

        BlockState sandstone = Blocks.SANDSTONE.getDefaultState();
        BlockState sand = Blocks.SAND.getDefaultState();
        BlockState barrier = Blocks.BARRIER.getDefaultState();
        int flags = Block.NOTIFY_LISTENERS;

        int outer = RADIUS + 2;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                int d2 = dx * dx + dz * dz;
                int x = ARENA_X + dx;
                int z = ARENA_Z + dz;
                if (d2 <= RADIUS * RADIUS) {
                    w.setBlockState(pos.set(x, ARENA_Y, z), sandstone, flags);
                    w.setBlockState(pos.set(x, ARENA_Y + 1, z), sand, flags);
                } else if (d2 <= outer * outer) {
                    for (int y = ARENA_Y + 1; y <= ARENA_Y + 14; y++) {
                        w.setBlockState(pos.set(x, y, z), barrier, flags);
                    }
                }
            }
        }
    }
}
