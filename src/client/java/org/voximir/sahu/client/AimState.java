package org.voximir.sahu.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.voximir.sahu.items.GunItem;
import org.voximir.sahu.packets.StartAimC2SPayload;
import org.voximir.sahu.packets.StopAimC2SPayload;

public final class AimState {

    private static final float TRANSITION_TICKS = 5.0f;
    private static final float ADS_FOV_MULTIPLIER = 0.75f;
    private static final float SNEAK_FOV_MULTIPLIER = 0.75f;
    private static final float EXPO_OUT_POWER = 3.0f;
    private static final float MAX_FOV_RANGE = 1.0f - (ADS_FOV_MULTIPLIER * SNEAK_FOV_MULTIPLIER);

    // Aim state (for animations / server sync)
    private static boolean aiming;

    // Unified FOV transition
    private static float fovMultiplier = 1.0f;
    private static float lastFovMultiplier = 1.0f;
    private static float fovTransitionStart;
    private static float fovTransitionTarget = 1.0f;
    private static float fovTransitionDuration;
    private static float fovTransitionElapsed;

    private AimState() {}

    private static float easeOutExpo(float progress) {
        if (progress <= 0.0f) {
            return 0.0f;
        }

        if (progress >= 1.0f) {
            return 1.0f;
        }

        double numerator = 1.0 - Math.pow(2.0, -EXPO_OUT_POWER * progress);
        double denominator = 1.0 - Math.pow(2.0, -EXPO_OUT_POWER);

        return (float) (numerator / denominator);
    }

    private static float computeTargetFovMultiplier(ClientPlayerEntity player, boolean holdingGun) {
        if (!holdingGun) {
            return 1.0f;
        }

        float target = 1.0f;

        if (aiming) {
            target *= ADS_FOV_MULTIPLIER;

            if (player.isSneaking()) {
                target *= SNEAK_FOV_MULTIPLIER;
            }
        }

        return target;
    }

    public static void tick(MinecraftClient client) {
        lastFovMultiplier = fovMultiplier;

        ClientPlayerEntity player = client.player;
        ItemStack stack = player == null ? ItemStack.EMPTY : player.getMainHandStack();
        boolean holdingGun = stack.getItem() instanceof GunItem;
        boolean desiredAiming = holdingGun
                && client.currentScreen == null
                && client.options.useKey.isPressed();

        if (player != null) {
            if (desiredAiming && !aiming) {
                ClientPlayNetworking.send(new StartAimC2SPayload());
            } else if (!desiredAiming && aiming) {
                ClientPlayNetworking.send(new StopAimC2SPayload());
            }
        }

        aiming = desiredAiming;

        // Compute the desired FOV multiplier based on current state
        float newTarget = player == null ? 1.0f : computeTargetFovMultiplier(player, holdingGun);

        // If the target changed, start a new transition from the current value
        if (newTarget != fovTransitionTarget) {
            fovTransitionStart = fovMultiplier;
            fovTransitionTarget = newTarget;
            float normalizedDistance = Math.abs(fovTransitionTarget - fovTransitionStart) / MAX_FOV_RANGE;
            fovTransitionDuration = Math.max(1.0f, TRANSITION_TICKS * normalizedDistance);
            fovTransitionElapsed = 0.0f;
        }

        // Advance the transition
        if (fovMultiplier != fovTransitionTarget) {
            fovTransitionElapsed = Math.min(fovTransitionDuration, fovTransitionElapsed + 1.0f);
            float progress = fovTransitionElapsed / fovTransitionDuration;
            float easedProgress = easeOutExpo(progress);
            fovMultiplier = fovTransitionStart + (fovTransitionTarget - fovTransitionStart) * easedProgress;
        } else {
            fovTransitionElapsed = 0.0f;
        }
    }

    public static boolean isAiming() {
        return aiming;
    }

    public static float applyGunFov(float baseFov, float tickDelta) {
        float multiplier = lastFovMultiplier + (fovMultiplier - lastFovMultiplier) * tickDelta;

        if (multiplier >= 1.0f) {
            return baseFov;
        }

        return baseFov * multiplier;
    }
}