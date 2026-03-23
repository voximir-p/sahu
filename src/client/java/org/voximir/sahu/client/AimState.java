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
    private static final float EXPO_OUT_POWER = 3.0f;

    private static boolean aiming;
    private static float aimProgress;
    private static float lastAimProgress;
    private static float transitionStartProgress;
    private static float transitionTargetProgress;
    private static float transitionDurationTicks;
    private static float transitionElapsedTicks;

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

    public static void tick(MinecraftClient client) {
        lastAimProgress = aimProgress;

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

        if (aiming != desiredAiming) {
            aiming = desiredAiming;
            transitionStartProgress = aimProgress;
            transitionTargetProgress = aiming ? 1.0f : 0.0f;
            transitionDurationTicks = Math.max(1.0f,
                    TRANSITION_TICKS * Math.abs(transitionTargetProgress - transitionStartProgress));
            transitionElapsedTicks = 0.0f;
        }

        if (aimProgress != transitionTargetProgress) {
            transitionElapsedTicks = Math.min(transitionDurationTicks, transitionElapsedTicks + 1.0f);
            float progress = transitionElapsedTicks / transitionDurationTicks;
            float easedProgress = easeOutExpo(progress);
            aimProgress = transitionStartProgress + (transitionTargetProgress - transitionStartProgress) * easedProgress;
        } else {
            transitionElapsedTicks = 0.0f;
        }
    }

    public static float getAimProgress(float tickDelta) {
        return lastAimProgress + (aimProgress - lastAimProgress) * tickDelta;
    }

    public static float applyAimFov(float baseFov, float tickDelta) {
        float progress = getAimProgress(tickDelta);

        if (progress <= 0.0f) {
            return baseFov;
        }

        float aimedFov = baseFov * ADS_FOV_MULTIPLIER;

        return baseFov + (aimedFov - baseFov) * progress;
    }
}