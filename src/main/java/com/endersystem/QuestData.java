package com.endersystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/** Today's daily quest progress for one player. */
public record QuestData(int day, int crouches, int jumps, int sprintCm, boolean claimed) {

    public static final int CROUCH_GOAL = 100;
    public static final int JUMP_GOAL = 100;
    public static final int SPRINT_BLOCKS_GOAL = 500;
    public static final int REWARD_POINTS = 9;

    public static final QuestData DEFAULT = new QuestData(0, 0, 0, 0, false);

    public static final Codec<QuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("day").forGetter(QuestData::day),
            Codec.INT.fieldOf("crouches").forGetter(QuestData::crouches),
            Codec.INT.fieldOf("jumps").forGetter(QuestData::jumps),
            Codec.INT.fieldOf("sprint_cm").forGetter(QuestData::sprintCm),
            Codec.BOOL.fieldOf("claimed").forGetter(QuestData::claimed)
    ).apply(i, QuestData::new));

    public static final PacketCodec<ByteBuf, QuestData> PACKET_CODEC = PacketCodecs.codec(CODEC);

    public static QuestData fresh(int day) {
        return new QuestData(day, 0, 0, 0, false);
    }

    public int sprintBlocks() {
        return sprintCm / 100;
    }

    public boolean complete() {
        return crouches >= CROUCH_GOAL && jumps >= JUMP_GOAL && sprintBlocks() >= SPRINT_BLOCKS_GOAL;
    }

    public QuestData addCrouches(int n) { return new QuestData(day, crouches + n, jumps, sprintCm, claimed); }
    public QuestData addJumps(int n) { return new QuestData(day, crouches, jumps + n, sprintCm, claimed); }
    public QuestData addSprintCm(int n) { return new QuestData(day, crouches, jumps, sprintCm + n, claimed); }
    public QuestData withClaimed() { return new QuestData(day, crouches, jumps, sprintCm, true); }
}
