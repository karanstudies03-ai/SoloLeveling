package com.endersystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderSystem implements ModInitializer {
    public static final String MOD_ID = "endersystem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.init();
        ModItems.init();
        ModAttachments.init();
        ModPackets.init();
        XpHandler.init();
        QuestTracker.init();
        Penalty.init();
        SystemCommand.init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                StatEffects.apply(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                StatEffects.apply(newPlayer));

        LOGGER.info("EnderSystem loaded.");
    }
}
