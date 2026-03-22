package org.voximir.sahu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import org.voximir.sahu.items.GunItem;
import org.voximir.sahu.packets.ReloadPacket;
import org.voximir.sahu.packets.StartFireC2SPayload;
import org.voximir.sahu.packets.StopFireC2SPayload;
import org.voximir.sahu.packets.SwitchPacket;

import static org.voximir.sahu.Sahu.LOGGER;

public class ModNetworking {

    public static void initialize() {
        // ── Start / Stop fire ────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                StartFireC2SPayload.ID, (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandStack().getItem();
                        if (item instanceof GunItem) {
                            FiringManager.startFiring(player.getUuid());
                        }
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                StopFireC2SPayload.ID, (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() ->
                            FiringManager.stopFiring(player.getUuid())
                    );
                }
        );

        // ── Reload ───────────────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                ReloadPacket.ID, (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandStack().getItem();
                        if (item instanceof GunItem gun) {
                            gun.tryReload(player);
                        }
                    });
                }
        );

        // ── Switch mode ──────────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                SwitchPacket.ID, (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandStack().getItem();

                        if (item instanceof GunItem gun) {
                            gun.switchMode(player);
                        }
                    });
                }
        );

        LOGGER.info("ModNetworking initialized");
    }
}
