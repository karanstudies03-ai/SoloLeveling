package com.endersystem;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Poison-Toothed Giant Desert Centipede. B-Rank, Insect.
 * Built on the silverfish model (an insect), scaled up with the SCALE attribute
 * and recoloured with its own texture. Its bite poisons (see Penalty.java).
 */
public class DesertCentipedeEntity extends SilverfishEntity {

    public DesertCentipedeEntity(EntityType<? extends SilverfishEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 60.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.ARMOR, 4.0)
                .add(EntityAttributes.SCALE, 5.0);
    }

    @Override
    protected void initGoals() {
        // Own goals (no silverfish "hide in blocks" behaviour)
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
}
