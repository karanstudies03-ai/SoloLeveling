package com.endersystem;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Turns stats into real gameplay effects. Every stat starts at 10 (= no bonus). */
public class StatEffects {
    private static final Identifier STR = Identifier.of(EnderSystem.MOD_ID, "stat_strength");
    private static final Identifier AGI = Identifier.of(EnderSystem.MOD_ID, "stat_agility");
    private static final Identifier VIT = Identifier.of(EnderSystem.MOD_ID, "stat_vitality");
    private static final Identifier DEF = Identifier.of(EnderSystem.MOD_ID, "stat_defence");
    private static final Identifier LUK = Identifier.of(EnderSystem.MOD_ID, "stat_luck");

    public static void apply(ServerPlayerEntity p) {
        SystemData d = p.getAttachedOrCreate(ModAttachments.SYSTEM);
        set(p, EntityAttributes.ATTACK_DAMAGE, STR, (d.strength() - 10) * 0.5,
                EntityAttributeModifier.Operation.ADD_VALUE);
        set(p, EntityAttributes.MOVEMENT_SPEED, AGI, (d.agility() - 10) * 0.01,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, EntityAttributes.MAX_HEALTH, VIT, (d.vitality() - 10) * 1.0,
                EntityAttributeModifier.Operation.ADD_VALUE);
        set(p, EntityAttributes.ARMOR, DEF, (d.defence() - 10) * 0.5,
                EntityAttributeModifier.Operation.ADD_VALUE);
        set(p, EntityAttributes.LUCK, LUK, (d.luck() - 10) * 0.2,
                EntityAttributeModifier.Operation.ADD_VALUE);
    }

    private static void set(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attribute,
                            Identifier id, double value, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0) inst.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
    }
}
