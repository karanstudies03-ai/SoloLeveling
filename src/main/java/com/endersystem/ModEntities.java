package com.endersystem;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<DesertCentipedeEntity> DESERT_CENTIPEDE = registerCentipede();

    private static EntityType<DesertCentipedeEntity> registerCentipede() {
        RegistryKey<EntityType<?>> key =
                RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnderSystem.MOD_ID, "desert_centipede"));
        EntityType<DesertCentipedeEntity> type = EntityType.Builder
                .create(DesertCentipedeEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.4f, 0.3f)
                .build(key);
        return Registry.register(Registries.ENTITY_TYPE, key, type);
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(DESERT_CENTIPEDE, DesertCentipedeEntity.createAttributes());
    }
}
