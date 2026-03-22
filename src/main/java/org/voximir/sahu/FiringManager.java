package org.voximir.sahu;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.voximir.sahu.items.GunItem;

import java.util.*;

import static org.voximir.sahu.Sahu.LOGGER;

public class FiringManager {

    private static final Set<UUID> firingPlayers = new HashSet<>();
    private static final Map<UUID, Integer> fireCooldowns = new HashMap<>();

    public static void startFiring(UUID uuid) {
        firingPlayers.add(uuid);
        fireCooldowns.put(uuid, 0);
    }

    public static void stopFiring(UUID uuid) {
        firingPlayers.remove(uuid);
        fireCooldowns.remove(uuid);
    }

    private static void tick(MinecraftServer server) {
        // ── Process firing players ───────────────────────────────────────
        Iterator<UUID> it = firingPlayers.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

            // Player left or died
            if (player == null || !player.isAlive()) {
                it.remove();
                fireCooldowns.remove(uuid);
                continue;
            }

            ItemStack stack = player.getMainHandStack();
            if (!(stack.getItem() instanceof GunItem gun)) {
                // No longer holding a gun
                it.remove();
                fireCooldowns.remove(uuid);
                continue;
            }

            int cooldown = fireCooldowns.getOrDefault(uuid, 0);
            if (cooldown > 0) {
                fireCooldowns.put(uuid, cooldown - 1);
                continue;
            }

            if (gun.tryFire(player)) {
                fireCooldowns.put(uuid, gun.getFireRate());
            }
        }

        // ── Process reload completion for ALL players holding a gun ──────
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack stack = player.getMainHandStack();
            if (stack.getItem() instanceof GunItem gun) {
                gun.tickReloadCompletion(player);
            }
        }
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(FiringManager::tick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            firingPlayers.remove(uuid);
            fireCooldowns.remove(uuid);
        });

        LOGGER.info("FiringManager initialized");
    }
}
