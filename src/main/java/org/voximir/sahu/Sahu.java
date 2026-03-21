package org.voximir.sahu;

import net.fabricmc.api.ModInitializer;

import java.util.logging.Logger;

public class Sahu implements ModInitializer {

    public static String MOD_ID = "sahu";
    public static Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello, Fabric!");
    }
}
