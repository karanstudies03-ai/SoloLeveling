package com.endersystem;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderSystem implements ModInitializer {
    public static final String MOD_ID = "endersystem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModAttachments.init();
        XpHandler.init();
        SystemCommand.init();
        LOGGER.info("EnderSystem Phase 1 loaded.");
    }
}
