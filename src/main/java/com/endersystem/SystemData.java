package com.endersystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** All System data for one player. Immutable: every change returns a new copy. */
public record SystemData(int level, int xp, int statPoints,
                         int strength, int agility, int vitality,
                         int intelligence, int defence, int luck) {

    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 3;

    public static final SystemData DEFAULT =
            new SystemData(1, 0, 0, 10, 10, 10, 10, 10, 10);

    public static final Codec<SystemData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(SystemData::level),
            Codec.INT.fieldOf("xp").forGetter(SystemData::xp),
            Codec.INT.fieldOf("points").forGetter(SystemData::statPoints),
            Codec.INT.fieldOf("str").forGetter(SystemData::strength),
            Codec.INT.fieldOf("agi").forGetter(SystemData::agility),
            Codec.INT.fieldOf("vit").forGetter(SystemData::vitality),
            Codec.INT.fieldOf("int").forGetter(SystemData::intelligence),
            Codec.INT.fieldOf("def").forGetter(SystemData::defence),
            Codec.INT.fieldOf("luk").forGetter(SystemData::luck)
    ).apply(i, SystemData::new));

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

    /** Adds XP and handles multiple level-ups. */
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
                intelligence, defence, luck);
    }
}
