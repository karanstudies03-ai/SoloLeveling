package com.endersystem;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ModPackets {

    /** Client -> Server: upgrade a stat (costs SystemData.UPGRADE_COST points). */
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

    /** Client -> Server: buy shop entry number `index`. */
    public record BuyPayload(int index) implements CustomPayload {
        public static final CustomPayload.Id<BuyPayload> ID =
                new CustomPayload.Id<>(Identifier.of(EnderSystem.MOD_ID, "buy"));
        public static final PacketCodec<RegistryByteBuf, BuyPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, BuyPayload::index, BuyPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Client -> Server: sell the item in inventory slot `slot` (one, or the whole stack). */
    public record SellPayload(int slot, boolean all) implements CustomPayload {
        public static final CustomPayload.Id<SellPayload> ID =
                new CustomPayload.Id<>(Identifier.of(EnderSystem.MOD_ID, "sell"));
        public static final PacketCodec<RegistryByteBuf, SellPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, SellPayload::slot,
                        PacketCodecs.BOOLEAN, SellPayload::all,
                        SellPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Server -> Client: show the hologram notification (level up, quest complete...). */
    public record NotifyPayload(String banner, String line1, String line2) implements CustomPayload {
        public static final CustomPayload.Id<NotifyPayload> ID =
                new CustomPayload.Id<>(Identifier.of(EnderSystem.MOD_ID, "notify"));
        public static final PacketCodec<RegistryByteBuf, NotifyPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, NotifyPayload::banner,
                        PacketCodecs.STRING, NotifyPayload::line1,
                        PacketCodecs.STRING, NotifyPayload::line2,
                        NotifyPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void notify(ServerPlayerEntity player, String banner, String line1, String line2) {
        ServerPlayNetworking.send(player, new NotifyPayload(banner, line1, line2));
    }

    private static void actionBar(ServerPlayerEntity player, String msg, Formatting color) {
        player.sendMessage(Text.literal("[SYSTEM] " + msg).formatted(color), true);
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(SpendPointPayload.ID, SpendPointPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyPayload.ID, BuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SellPayload.ID, SellPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NotifyPayload.ID, NotifyPayload.CODEC);

        // ---- Upgrade a stat ----
        ServerPlayNetworking.registerGlobalReceiver(SpendPointPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            SystemData before = player.getAttachedOrCreate(ModAttachments.SYSTEM);
            SystemData after = before.withStatAdded(payload.stat());
            if (after != before) {
                player.setAttached(ModAttachments.SYSTEM, after);
                StatEffects.apply(player);
            }
        });

        // ---- Buy from the shop ----
        ServerPlayNetworking.registerGlobalReceiver(BuyPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int idx = payload.index();
            if (idx < 0 || idx >= ShopData.BUY.size()) return;
            ShopData.BuyEntry entry = ShopData.BUY.get(idx);
            SystemData data = player.getAttachedOrCreate(ModAttachments.SYSTEM);
            if (data.gold() < entry.price()) {
                actionBar(player, "Not enough gold!", Formatting.RED);
                return;
            }
            player.setAttached(ModAttachments.SYSTEM, data.addGold(-entry.price()));
            ItemStack bought = new ItemStack(entry.item());
            String name = bought.getName().getString();
            player.getInventory().offerOrDrop(bought);
            actionBar(player, "Bought " + name + " for " + entry.price() + " gold", Formatting.GOLD);
        });

        // ---- Sell from the inventory ----
        ServerPlayNetworking.registerGlobalReceiver(SellPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int slot = payload.slot();
            if (slot < 0 || slot > 35) return;
            PlayerInventory inv = player.getInventory();
            ItemStack stack = inv.getStack(slot);
            if (stack.isEmpty()) return;
            int price = ShopData.sellPrice(stack.getItem());
            if (price <= 0) return;
            int count = payload.all() ? stack.getCount() : 1;
            String name = stack.getName().getString();
            inv.removeStack(slot, count);
            int total = price * count;
            SystemData data = player.getAttachedOrCreate(ModAttachments.SYSTEM);
            player.setAttached(ModAttachments.SYSTEM, data.addGold(total));
            actionBar(player, "Sold " + count + "x " + name + " for " + total + " gold", Formatting.GOLD);
        });
    }
}
