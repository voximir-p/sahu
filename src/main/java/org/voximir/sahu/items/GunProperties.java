package org.voximir.sahu.items;

public record GunProperties(
        int maxAmmo,
        int fireRate,
        double range,
        float baseDamage,
        float headshotMultiplier,
        float recoilPitch,
        float recoilYaw,
        int recoilDuration,
        int tacticalReloadDuration,
        int fullReloadDuration,
        GunSounds sounds
) {
    public int getReloadDuration(boolean hasAmmo) {
        return hasAmmo ? tacticalReloadDuration : fullReloadDuration;
    }
}
