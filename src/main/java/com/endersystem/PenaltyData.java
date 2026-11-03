package com.endersystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/** State of an active Penalty Quest, including where to send the player back to. */
public record PenaltyData(boolean active, int ticksLeft, String dim,
                          double rx, double ry, double rz, float yaw, float pitch) {

    /** 2 minutes */
    public static final int DURATION_TICKS = 2 * 60 * 20;

    public static final PenaltyData NONE =
            new PenaltyData(false, 0, "minecraft:overworld", 0, 0, 0, 0f, 0f);

    public static final Codec<PenaltyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("active").forGetter(PenaltyData::active),
            Codec.INT.fieldOf("ticks_left").forGetter(PenaltyData::ticksLeft),
            Codec.STRING.fieldOf("dim").forGetter(PenaltyData::dim),
            Codec.DOUBLE.fieldOf("rx").forGetter(PenaltyData::rx),
            Codec.DOUBLE.fieldOf("ry").forGetter(PenaltyData::ry),
            Codec.DOUBLE.fieldOf("rz").forGetter(PenaltyData::rz),
            Codec.FLOAT.fieldOf("yaw").forGetter(PenaltyData::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(PenaltyData::pitch)
    ).apply(i, PenaltyData::new));

    public static final PacketCodec<ByteBuf, PenaltyData> PACKET_CODEC = PacketCodecs.codec(CODEC);

    public PenaltyData withTicks(int ticks) {
        return new PenaltyData(active, ticks, dim, rx, ry, rz, yaw, pitch);
    }
}
