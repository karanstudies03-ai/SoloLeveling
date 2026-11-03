package com.endersystem;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

public class ModAttachments {
    /** Saved with the player, kept after death, synced ONLY to that player. */
    public static final AttachmentType<SystemData> SYSTEM = AttachmentRegistry.create(
            Identifier.of(EnderSystem.MOD_ID, "system_data"),
            builder -> builder
                    .initializer(() -> SystemData.DEFAULT)
                    .persistent(SystemData.CODEC)
                    .copyOnDeath()
                    .syncWith(SystemData.PACKET_CODEC, AttachmentSyncPredicate.targetOnly()));

    public static final AttachmentType<QuestData> QUESTS = AttachmentRegistry.create(
            Identifier.of(EnderSystem.MOD_ID, "quest_data"),
            builder -> builder
                    .initializer(() -> QuestData.DEFAULT)
                    .persistent(QuestData.CODEC)
                    .copyOnDeath()
                    .syncWith(QuestData.PACKET_CODEC, AttachmentSyncPredicate.targetOnly()));

    public static void init() {
        // Loading this class registers the attachments.
    }
}
