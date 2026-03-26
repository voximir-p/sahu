package org.voximir.sahu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.voximir.sahu.items.GunItem;
import org.voximir.sahu.packets.ReloadC2SPayload;
import org.voximir.sahu.packets.StartAimC2SPayload;
import org.voximir.sahu.packets.StartFireC2SPayload;
import org.voximir.sahu.packets.StopAimC2SPayload;
import org.voximir.sahu.packets.StopFireC2SPayload;
import org.voximir.sahu.packets.SwitchFireModeC2SPayload;

import static org.voximir.sahu.Sahu.LOGGER;

public class ModNetworking {

    public static void initialize() {
        // ── Start / Stop aim ─────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                StartAimC2SPayload.ID, (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandItem().getItem();

                        if (item instanceof GunItem gun) {
                            gun.startAim(player);
                        }
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                StopAimC2SPayload.ID, (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandItem().getItem();

                        if (item instanceof GunItem gun) {
                            gun.stopAim(player);
                        }
                    });
                }
        );

        // ── Start / Stop fire ────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                StartFireC2SPayload.ID, (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandItem().getItem();
                        if (item instanceof GunItem) {
                            FiringManager.startFiring(player.getUUID());
                        }
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                StopFireC2SPayload.ID, (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() ->
                            FiringManager.stopFiring(player.getUUID())
                    );
                }
        );

        // ── Reload ───────────────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                ReloadC2SPayload.ID, (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandItem().getItem();
                        if (item instanceof GunItem gun) {
                            gun.tryReload(player);
                        }
                    });
                }
        );

        // ── Switch mode ──────────────────────────────────────────────────
        ServerPlayNetworking.registerGlobalReceiver(
                SwitchFireModeC2SPayload.ID, (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> {
                        Item item = player.getMainHandItem().getItem();

                        if (item instanceof GunItem gun) {
                            gun.switchMode(player);
                        }
                    });
                }
        );

        LOGGER.info("ModNetworking initialized");
    }
}
