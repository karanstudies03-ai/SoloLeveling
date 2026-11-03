package com.endersystem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static final Item BEAST_CORE = register("beast_core", Rarity.UNCOMMON);
    public static final Item DEMON_HEART = register("demon_heart", Rarity.RARE);
    public static final Item DRAGON_SCALE = register("dragon_scale", Rarity.RARE);
    public static final Item SHADOW_ESSENCE = register("shadow_essence", Rarity.RARE);
    public static final Item MONARCH_CORE = register("monarch_core", Rarity.EPIC);

    private static Item register(String name, Rarity rarity) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EnderSystem.MOD_ID, name));
        return Registry.register(Registries.ITEM, key, new Item(new Item.Settings().registryKey(key).rarity(rarity)));
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(BEAST_CORE);
            entries.add(DEMON_HEART);
            entries.add(DRAGON_SCALE);
            entries.add(SHADOW_ESSENCE);
            entries.add(MONARCH_CORE);
        });
    }
}
