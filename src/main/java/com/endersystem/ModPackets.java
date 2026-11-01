package com.endersystem;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModPackets {

    /** Client -> Server: "spend one point on this stat". */
    public record SpendPointPayload(int stat) implements CustomPayload {
        public static final CustomPayload.Id<SpendPointPayload> ID =
                new CustomPayload.Id<>(Identifier.of(EnderSystem.MOD_ID, "spend_point"));
        public static final PacketCodec<RegistryByteBuf, SpendPointPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, SpendPointPayload::stat, SpendPointPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Server -> Client: "show the LEVEL UP hologram". */
    public record LevelUpPayload(int level, int pointsGained) implements CustomPayload {
        public static final CustomPayload.Id<LevelUpPayload> ID =
                new CustomPayload.Id<>(Identifier.of(EnderSystem.MOD_ID, "level_up"));
        public static final PacketCodec<RegistryByteBuf, LevelUpPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, LevelUpPayload::level,
                        PacketCodecs.VAR_INT, LevelUpPayload::pointsGained,
                        LevelUpPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(SpendPointPayload.ID, SpendPointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LevelUpPayload.ID, LevelUpPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SpendPointPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            SystemData before = player.getAttachedOrCreate(ModAttachments.SYSTEM);
            SystemData after = before.withStatAdded(payload.stat());
            if (after != before) {
                player.setAttached(ModAttachments.SYSTEM, after);
                StatEffects.apply(player);
            }
        });
    }
}
