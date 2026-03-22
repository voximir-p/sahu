package org.voximir.sahu.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.voximir.sahu.packets.RecoilS2CPayload;

public class SahuClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModKeyMappings.initialize();
        ClientEvents.initialize();
        AmmoHudOverlay.initialize();

        // S2C packet handlers
        ClientPlayNetworking.registerGlobalReceiver(
                RecoilS2CPayload.ID, (payload, context) ->
                        context.client().execute(() ->
                                RecoilHandler.apply(
                                        payload.pitchKick(),
                                        payload.yawKick(),
                                        payload.duration()
                                )
                        )
        );
    }
}
