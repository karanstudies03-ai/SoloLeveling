package com.endersystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
 * The quest resets every real-world day. Reward is given once when all three are done.
 */
public class QuestTracker {
    private static final Set<UUID> SNEAKING = new HashSet<>();
    private static final Map<UUID, Integer> LAST_JUMP = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SPRINT = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                tick(p);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            SNEAKING.remove(id);
            LAST_JUMP.remove(id);
            LAST_SPRINT.remove(id);
        });
    }

    private static void tick(ServerPlayerEntity p) {
        QuestData original = p.getAttachedOrCreate(ModAttachments.QUESTS);
        QuestData q = original;

        int today = (int) LocalDate.now().toEpochDay();
        if (q.day() != today) {
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
    }
}
