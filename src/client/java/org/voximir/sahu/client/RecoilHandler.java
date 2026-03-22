package org.voximir.sahu.client;

import net.minecraft.client.MinecraftClient;

/**
 * Frame-interpolated recoil system.
 * <p>
 * Kick phase:  applies the full recoil over {@code durationTicks} using
 *              an ease-out cubic curve (fast snap, soft finish).
 * Recovery phase: settles back ~35 % of the kick over a few ticks.
 * <p>
 * {@link #tick} only advances the tick counter.
 * {@link #renderFrame} is called every frame and interpolates smoothly.
 */
public class RecoilHandler {

    private static final float RECOVERY_FRACTION = 0.35f;
    private static final int   RECOVERY_TICKS    = 4;

    // ── Kick state ──────────────────────────────────────────────────────
    private static float totalPitch = 0f;
    private static float totalYaw   = 0f;
    private static int   durationTicks = 0;
    private static int   elapsedTicks  = 0;
    private static float appliedFraction = 0f; // how much of the ease curve we've output so far

    // ── Recovery state ──────────────────────────────────────────────────
    private static float recoveryPitch = 0f;
    private static float recoveryYaw   = 0f;
    private static int   recoveryTicksLeft = 0;
    private static float recoveryApplied   = 0f;

    /**
     * Called from packet handler when a new shot recoil arrives.
     * Accumulates on top of any in-progress kick.
     */
    public static void apply(float pitch, float yaw, int duration) {
        // If there's leftover from a previous kick, flush it instantly
        if (durationTicks > 0 && appliedFraction < 1f) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                float remaining = 1f - appliedFraction;
                client.player.setPitch(client.player.getPitch() + totalPitch * remaining);
                client.player.setYaw(client.player.getYaw() + totalYaw * remaining);
            }
        }

        totalPitch = pitch;
        totalYaw   = yaw;
        durationTicks = duration;
        elapsedTicks  = 0;
        appliedFraction = 0f;

        // Queue the recovery for after the kick finishes
        recoveryPitch = -pitch * RECOVERY_FRACTION;
        recoveryYaw   = -yaw   * RECOVERY_FRACTION;
        recoveryTicksLeft = 0; // will start after kick ends
        recoveryApplied   = 0f;
    }

    /**
     * Called once per client tick — only advances the elapsed counter.
     * NO rotation changes happen here.
     */
    public static void tick(MinecraftClient client) {
        if (durationTicks > 0 && elapsedTicks < durationTicks) {
            elapsedTicks++;
            // Kick just finished — start recovery
            if (elapsedTicks >= durationTicks) {
                recoveryTicksLeft = RECOVERY_TICKS;
                recoveryApplied = 0f;
            }
        } else if (recoveryTicksLeft > 0) {
            recoveryTicksLeft--;
        }
    }

    /**
     * Called every frame from the GameRenderer mixin.
     * Interpolates rotation using partial ticks for buttery-smooth results.
     */
    public static void renderFrame(MinecraftClient client, float tickDelta) {
        if (client.player == null) return;

        // ── Kick phase ─────────────────────────────────────────────────
        if (durationTicks > 0 && appliedFraction < 1f) {
            float progress = Math.min((elapsedTicks + tickDelta) / durationTicks, 1f);
            float eased = easeOutCubic(progress);

            float delta = eased - appliedFraction;
            if (delta > 0f) {
                client.player.setPitch(client.player.getPitch() + totalPitch * delta);
                client.player.setYaw(client.player.getYaw()   + totalYaw   * delta);
                appliedFraction = eased;
            }
        }

        // ── Recovery phase (settle back) ───────────────────────────────
        if (recoveryTicksLeft > 0 || recoveryApplied < 1f) {
            int totalRecovery = RECOVERY_TICKS;
            int recoveryElapsed = totalRecovery - recoveryTicksLeft;
            float rProgress = Math.min((recoveryElapsed + tickDelta) / totalRecovery, 1f);
            float rEased = easeOutQuad(rProgress);

            float rDelta = rEased - recoveryApplied;
            if (rDelta > 0f && (recoveryPitch != 0f || recoveryYaw != 0f)) {
                client.player.setPitch(client.player.getPitch() + recoveryPitch * rDelta);
                client.player.setYaw(client.player.getYaw()   + recoveryYaw   * rDelta);
                recoveryApplied = rEased;
            }
        }
    }

    // ── Easing functions ────────────────────────────────────────────────

    /** Fast start, soft finish — gives punchy initial kick */
    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Gentler ease-out for recovery settle */
    private static float easeOutQuad(float t) {
        return t * (2f - t);
    }
}

