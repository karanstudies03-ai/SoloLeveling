package com.endersystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/** All System data for one player. Immutable: every change returns a new copy. */
public record SystemData(int level, int xp, int statPoints,
                         int strength, int agility, int vitality,
                         int intelligence, int defence, int luck,
                         int gold) {

    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 3;
    /** Points needed to raise one stat by +1. */
    public static final int UPGRADE_COST = 3;
    public static final String[] STAT_NAMES = {"STR", "AGI", "VIT", "INT", "DEF", "LUK"};

    public static final SystemData DEFAULT =
            new SystemData(1, 0, 0, 10, 10, 10, 10, 10, 10, 0);

    public static final Codec<SystemData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(SystemData::level),
            Codec.INT.fieldOf("xp").forGetter(SystemData::xp),
            Codec.INT.fieldOf("points").forGetter(SystemData::statPoints),
            Codec.INT.fieldOf("str").forGetter(SystemData::strength),
            Codec.INT.fieldOf("agi").forGetter(SystemData::agility),
            Codec.INT.fieldOf("vit").forGetter(SystemData::vitality),
            Codec.INT.fieldOf("int").forGetter(SystemData::intelligence),
            Codec.INT.fieldOf("def").forGetter(SystemData::defence),
            Codec.INT.fieldOf("luk").forGetter(SystemData::luck),
            Codec.INT.optionalFieldOf("gold", 0).forGetter(SystemData::gold)
    ).apply(i, SystemData::new));

    public static final PacketCodec<ByteBuf, SystemData> PACKET_CODEC = PacketCodecs.codec(CODEC);

    public static int xpForNextLevel(int level) {
        return 100 + level * 50;
    }

    public static String rankFor(int level) {
        if (level <= 10) return "E";
        if (level <= 25) return "D";
        if (level <= 40) return "C";
        if (level <= 60) return "B";
        if (level <= 80) return "A";
        return "S";
    }

    public String rank() {
        return rankFor(level);
    }

    /** stat index: 0 STR, 1 AGI, 2 VIT, 3 INT, 4 DEF, 5 LUK */
    public int stat(int index) {
        return switch (index) {
            case 0 -> strength;
            case 1 -> agility;
            case 2 -> vitality;
            case 3 -> intelligence;
            case 4 -> defence;
            default -> luck;
        };
    }

    /** Spends UPGRADE_COST points for +1 on a stat. Returns this (same instance) if not possible. */
    public SystemData withStatAdded(int index) {
        if (statPoints < UPGRADE_COST || index < 0 || index > 5) return this;
        return new SystemData(level, xp, statPoints - UPGRADE_COST,
                strength + (index == 0 ? 1 : 0),
                agility + (index == 1 ? 1 : 0),
                vitality + (index == 2 ? 1 : 0),
                intelligence + (index == 3 ? 1 : 0),
                defence + (index == 4 ? 1 : 0),
                luck + (index == 5 ? 1 : 0),
                gold);
    }

    public SystemData addPoints(int n) {
        return new SystemData(level, xp, statPoints + n, strength, agility, vitality,
                intelligence, defence, luck, gold);
    }

    public SystemData addGold(int n) {
        return new SystemData(level, xp, statPoints, strength, agility, vitality,
                intelligence, defence, luck, Math.max(0, gold + n));
    }

    public SystemData addXp(int amount) {
        if (level >= MAX_LEVEL) return this;
        int lvl = level, x = xp + amount, pts = statPoints;
        while (lvl < MAX_LEVEL && x >= xpForNextLevel(lvl)) {
            x -= xpForNextLevel(lvl);
            lvl++;
            pts += POINTS_PER_LEVEL;
        }
        if (lvl >= MAX_LEVEL) x = 0;
        return new SystemData(lvl, x, pts, strength, agility, vitality,
                intelligence, defence, luck, gold);
    }
}
