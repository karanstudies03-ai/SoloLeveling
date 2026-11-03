package com.endersystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks the daily quest on the server:
 *  - crouch: counts every time the player STARTS crouching
 *  - jump:   vanilla jump statistic
 *  - sprint: vanilla sprint-distance statistic
 * The quest resets at midnight (the server's local time). If the previous day's quest was
 * not completed, the player gets the Penalty Quest (see Penalty.java).
 */
public class QuestTracker {
    private static final Set<UUID> SNEAKING = new HashSet<>();
    private static final Map<UUID, Integer> LAST_JUMP = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SPRINT = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                tick(server, p);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            SNEAKING.remove(id);
            LAST_JUMP.remove(id);
            LAST_SPRINT.remove(id);
        });
    }

    private static void tick(MinecraftServer server, ServerPlayerEntity p) {
        if (!p.isAlive()) return;

        QuestData original = p.getAttachedOrCreate(ModAttachments.QUESTS);
        QuestData q = original;

        // Midnight reset (calendar day changed)
        int today = (int) LocalDate.now().toEpochDay();
        boolean missed = false;
        if (q.day() != today) {
            // Penalty only if the player was around yesterday (day 0 = brand new player)
            missed = q.day() != 0 && today - q.day() == 1 && !q.claimed();
            q = QuestData.fresh(today);
        }

        UUID id = p.getUuid();

        // Crouch: count the moment the player starts sneaking
        boolean sneaking = p.isSneaking();
        boolean wasSneaking = SNEAKING.contains(id);
        if (sneaking && !wasSneaking) q = q.addCrouches(1);
        if (sneaking) SNEAKING.add(id); else SNEAKING.remove(id);

        // Jump + sprint from vanilla statistics
        StatHandler stats = p.getStatHandler();
        int jump = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.JUMP));
        int sprint = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM));
        Integer lastJump = LAST_JUMP.put(id, jump);
        Integer lastSprint = LAST_SPRINT.put(id, sprint);
        if (lastJump != null && jump > lastJump) q = q.addJumps(jump - lastJump);
        if (lastSprint != null && sprint > lastSprint) q = q.addSprintCm(sprint - lastSprint);

        // Reward
        if (!q.claimed() && q.complete()) {
            q = q.withClaimed();
            SystemData d = p.getAttachedOrCreate(ModAttachments.SYSTEM);
            p.setAttached(ModAttachments.SYSTEM, d.addPoints(QuestData.REWARD_POINTS));
            ModPackets.notify(p, "QUEST COMPLETE!", "Daily Quest cleared",
                    "+" + QuestData.REWARD_POINTS + " stat points");
        }

        if (!q.equals(original)) {
            p.setAttached(ModAttachments.QUESTS, q);
        }

        if (missed) {
            Penalty.start(server, p, false);
        }
    }
}
