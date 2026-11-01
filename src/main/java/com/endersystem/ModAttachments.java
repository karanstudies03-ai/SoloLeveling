package com.endersystem;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

public class ModAttachments {
    /** Saved with the player, and kept after death. */
    public static final AttachmentType<SystemData> SYSTEM = AttachmentRegistry.create(
            Identifier.of(EnderSystem.MOD_ID, "system_data"),
            builder -> builder
                    .initializer(() -> SystemData.DEFAULT)
                    .persistent(SystemData.CODEC)
                    .copyOnDeath());

    public static void init() {
        // Loading this class registers the attachment.
    }
}
