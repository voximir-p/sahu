package org.voximir.sahu;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.voximir.sahu.packets.RecoilS2CPayload;
import org.voximir.sahu.packets.ReloadPacket;
import org.voximir.sahu.packets.StartFireC2SPayload;
import org.voximir.sahu.packets.StopFireC2SPayload;
import org.voximir.sahu.packets.SwitchPacket;

import static org.voximir.sahu.Sahu.LOGGER;

public class ModPackets {

    public static void initialize() {
        // C2S
        PayloadTypeRegistry.playC2S().register(StartFireC2SPayload.ID, StartFireC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopFireC2SPayload.ID, StopFireC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReloadPacket.ID, ReloadPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwitchPacket.ID, SwitchPacket.CODEC);

        // S2C
        PayloadTypeRegistry.playS2C().register(RecoilS2CPayload.ID, RecoilS2CPayload.CODEC);

        LOGGER.info("ModPackets initialized");
    }
}
