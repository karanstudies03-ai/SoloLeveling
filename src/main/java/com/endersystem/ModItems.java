package com.endersystem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;

/** The shop materials. Eat or drink them (right-click) for a 30 second Level X buff. */
public class ModItems {
    /** Effect level shown to the player is BUFF_AMPLIFIER + 1 (9 = level 10). */
    private static final int BUFF_AMPLIFIER = 9;
    /** Speed is weaker: level 7 (amplifier 6). */
    private static final int SPEED_AMPLIFIER = 6;
    private static final int BUFF_SECONDS = 30;

    public static final Item BEAST_CORE = register("beast_core", Rarity.UNCOMMON,
            food(fx(StatusEffects.STRENGTH)));
    public static final Item DEMON_HEART = register("demon_heart", Rarity.RARE,
            food(fx(StatusEffects.REGENERATION)));
    public static final Item DRAGON_SCALE = register("dragon_scale", Rarity.RARE,
            food(fx(StatusEffects.RESISTANCE)));
    public static final Item SHADOW_ESSENCE = register("shadow_essence", Rarity.RARE,
            drink(speed()));
    public static final Item MONARCH_CORE = register("monarch_core", Rarity.EPIC,
            drink(fx(StatusEffects.STRENGTH), fx(StatusEffects.REGENERATION),
                    fx(StatusEffects.RESISTANCE), speed()));

    private static StatusEffectInstance fx(RegistryEntry<StatusEffect> effect) {
        return new StatusEffectInstance(effect, BUFF_SECONDS * 20, BUFF_AMPLIFIER);
    }

    private static StatusEffectInstance speed() {
        return new StatusEffectInstance(StatusEffects.SPEED, BUFF_SECONDS * 20, SPEED_AMPLIFIER);
    }

    private static ConsumableComponent food(StatusEffectInstance... effects) {
        return ConsumableComponents.food()
                .consumeEffect(new ApplyEffectsConsumeEffect(List.of(effects)))
                .build();
    }

    private static ConsumableComponent drink(StatusEffectInstance... effects) {
        return ConsumableComponents.drink()
                .consumeEffect(new ApplyEffectsConsumeEffect(List.of(effects)))
                .build();
    }

    private static Item register(String name, Rarity rarity, ConsumableComponent consumable) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EnderSystem.MOD_ID, name));
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .rarity(rarity)
                .component(DataComponentTypes.CONSUMABLE, consumable);
        return Registry.register(Registries.ITEM, key, new Item(settings));
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
