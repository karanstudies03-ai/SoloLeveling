package com.endersystem;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shop buy list and item sell prices. Used by both server (to check) and client (to display). */
public final class ShopData {

    public record BuyEntry(Item item, String usage, int price) {}

    /** The materials from the design doc. Effects of the materials come in later phases. */
    public static final List<BuyEntry> BUY = List.of(
            new BuyEntry(ModItems.BEAST_CORE, "Upgrade equipment", 200),
            new BuyEntry(ModItems.DEMON_HEART, "Unlock magic", 600),
            new BuyEntry(ModItems.DRAGON_SCALE, "Craft armour", 800),
            new BuyEntry(ModItems.SHADOW_ESSENCE, "Upgrade shadows", 500),
            new BuyEntry(ModItems.MONARCH_CORE, "Unlock ultimate skills", 3000)
    );

    private static final Map<Item, Integer> SELL = new HashMap<>();

    private static void sell(Item item, int price) {
        SELL.put(item, price);
    }

    static {
        // Our materials sell for 25% of their shop price
        for (BuyEntry e : BUY) sell(e.item(), e.price() / 4);

        // Common drops and ores (gold per item). Add more here any time.
        sell(Items.COAL, 1);
        sell(Items.CHARCOAL, 1);
        sell(Items.COPPER_INGOT, 2);
        sell(Items.IRON_INGOT, 4);
        sell(Items.GOLD_INGOT, 8);
        sell(Items.REDSTONE, 1);
        sell(Items.LAPIS_LAZULI, 2);
        sell(Items.QUARTZ, 2);
        sell(Items.EMERALD, 20);
        sell(Items.DIAMOND, 40);
        sell(Items.NETHERITE_SCRAP, 60);
        sell(Items.NETHERITE_INGOT, 300);
        sell(Items.ROTTEN_FLESH, 1);
        sell(Items.BONE, 1);
        sell(Items.STRING, 1);
        sell(Items.GUNPOWDER, 2);
        sell(Items.SPIDER_EYE, 2);
        sell(Items.SLIME_BALL, 3);
        sell(Items.PHANTOM_MEMBRANE, 5);
        sell(Items.ENDER_PEARL, 10);
        sell(Items.BLAZE_ROD, 8);
        sell(Items.GHAST_TEAR, 20);
        sell(Items.NETHER_STAR, 500);
    }

    /** Gold per single item, or 0 if the item cannot be sold. */
    public static int sellPrice(Item item) {
        return SELL.getOrDefault(item, 0);
    }

    private ShopData() {}
}
