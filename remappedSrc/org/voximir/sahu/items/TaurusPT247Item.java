package org.voximir.sahu.items;

import org.voximir.sahu.ModSoundEvents;

public class TaurusPT247Item extends GunItem {

    public static final GunProperties PROPERTIES = new GunProperties(
            17,     // maxAmmo
            2,      // fireRate
            50.0,   // range
            8.0f,   // baseDamage
            2.0f,   // headshotMultiplier
            0.75f,  // hipFireAccuracy
            0.90f,  // aimedAccuracy
            -12.0f, // recoilPitch
            2.5f,   // recoilYaw
            4,      // recoilDuration
            19,     // tacticalReloadDuration
            30,     // fullReloadDuration
            ModSoundEvents.TAURUS_PT_24_7_SOUNDS
    );

    public TaurusPT247Item(Properties settings) {
        super(settings, PROPERTIES);
    }
}
