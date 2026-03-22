package org.voximir.sahu.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.Item;
import org.voximir.sahu.items.GunItem;
import org.voximir.sahu.packets.ReloadC2SPayload;
import org.voximir.sahu.packets.StartFireC2SPayload;
import org.voximir.sahu.packets.StopFireC2SPayload;
import org.voximir.sahu.packets.SwitchFireModeC2SPayload;

import static org.voximir.sahu.client.ModKeyMappings.RELOAD_KEYBIND;
import static org.voximir.sahu.client.ModKeyMappings.SWITCH_KEYBIND;

public class ClientEvents {

    private static boolean wasFiring = false;

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Reset state when player is unavailable
            if (client.player == null || client.getNetworkHandler() == null) {
                wasFiring = false;
                return;
            }

            Item item = client.player.getMainHandStack().getItem();
            boolean holdingGun = item instanceof GunItem;

            // Determine if the player should be firing right now
            boolean shouldFire = holdingGun
                    && client.currentScreen == null
                    && client.options.attackKey.isPressed();

            // Send start/stop packets only on transitions
            if (shouldFire && !wasFiring) {
                ClientPlayNetworking.send(new StartFireC2SPayload());
            } else if (!shouldFire && wasFiring) {
                ClientPlayNetworking.send(new StopFireC2SPayload());
            }
            wasFiring = shouldFire;

            // Reload / switch — wasPressed() so they only fire once per key press
            if (holdingGun) {
                while (RELOAD_KEYBIND.wasPressed()) {
                    ClientPlayNetworking.send(new ReloadC2SPayload());
                }
                while (SWITCH_KEYBIND.wasPressed()) {
                    ClientPlayNetworking.send(new SwitchFireModeC2SPayload());
                }
            }
        });

        // Recoil + screen-shake tick handlers
        ClientTickEvents.END_CLIENT_TICK.register(RecoilHandler::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ScreenShakeHandler::tick);
    }
}
