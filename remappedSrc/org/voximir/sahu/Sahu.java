package org.voximir.sahu;

import java.util.logging.Logger;

import net.fabricmc.api.ModInitializer;

public class Sahu implements ModInitializer {

    public static final String MOD_ID = "sahu";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModDataComponents.initialize();
        ModSoundEvents.initialize();
        ModItems.initialize();
        ModItemGroups.initialize();

        ModPackets.initialize();
        ModNetworking.initialize();
        FiringManager.initialize();

        LOGGER.info("Sahu mod initialized");
    }
}
