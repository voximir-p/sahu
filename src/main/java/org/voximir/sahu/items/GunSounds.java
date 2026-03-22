package org.voximir.sahu.items;

import net.minecraft.sound.SoundEvent;

public record GunSounds(
        SoundEvent shootSound,
        SoundEvent emptySound,
        SoundEvent switchSound,
        SoundEvent tacticalReloadSound,
        SoundEvent fullReloadSound
) {
    public SoundEvent getReloadSound(boolean hasAmmo) {
        return hasAmmo ? tacticalReloadSound : fullReloadSound;
    }
}
