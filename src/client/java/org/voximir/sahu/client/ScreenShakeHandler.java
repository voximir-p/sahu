package org.voximir.sahu.client;

import net.minecraft.client.MinecraftClient;

public class ScreenShakeHandler {

    private static float shakeIntensity = 0f;
    private static int shakeTicks = 0;

    public static void shake(float intensity, int duration) {
        shakeIntensity = intensity;
        shakeTicks = duration;
    }

    public static void tick(MinecraftClient client) {
        if (shakeTicks > 0 && client.player != null) {
            shakeTicks--;

            float progress = (float) shakeTicks / 10.0f;

            if (client.world != null) {
                float offsetYaw = (client.world.random.nextFloat() - 0.5f) * shakeIntensity * progress;
                float offsetPitch = (client.world.random.nextFloat() - 0.5f) * shakeIntensity * progress;

                client.player.setYaw(client.player.getYaw() + offsetYaw);
                client.player.setPitch(client.player.getPitch() + offsetPitch);
            }
        }
    }
}