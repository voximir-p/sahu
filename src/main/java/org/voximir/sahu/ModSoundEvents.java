package org.voximir.sahu;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.LOGGER;
import static org.voximir.sahu.Sahu.MOD_ID;

public class ModSoundEvents {

    public static final Identifier TAURUS_PT_2_47_MAGIN_ID = Identifier.of(MOD_ID, "taurus_pt_24_7_magin");
    public static final SoundEvent TAURUS_PT_2_47_MAGIN = SoundEvent.of(TAURUS_PT_2_47_MAGIN_ID);

    public static final Identifier TAURUS_PT_2_47_RELOAD_ID = Identifier.of(MOD_ID, "taurus_pt_24_7_reload");
    public static final SoundEvent TAURUS_PT_2_47_RELOAD = SoundEvent.of(TAURUS_PT_2_47_RELOAD_ID);

    public static final Identifier TAURUS_PT_2_47_SHOOT_ID = Identifier.of(MOD_ID, "taurus_pt_24_7_shoot");
    public static final SoundEvent TAURUS_PT_2_47_SHOOT = SoundEvent.of(TAURUS_PT_2_47_SHOOT_ID);

    public static final Identifier TAURUS_PT_2_47_SWITCH_ID = Identifier.of(MOD_ID, "taurus_pt_24_7_switch");
    public static final SoundEvent TAURUS_PT_2_47_SWITCH = SoundEvent.of(TAURUS_PT_2_47_SWITCH_ID);

    public static final Identifier TAURUS_PT_2_47_EMPTY_ID = Identifier.of(MOD_ID, "taurus_pt_24_7_empty");
    public static final SoundEvent TAURUS_PT_2_47_EMPTY = SoundEvent.of(TAURUS_PT_2_47_EMPTY_ID);

    private static void registerSound(Identifier identifier, SoundEvent soundevent) {
        Registry.register(Registries.SOUND_EVENT, identifier, soundevent);
    }

    public static void initialize() {
        registerSound(TAURUS_PT_2_47_MAGIN_ID, TAURUS_PT_2_47_MAGIN);
        registerSound(TAURUS_PT_2_47_RELOAD_ID, TAURUS_PT_2_47_RELOAD);
        registerSound(TAURUS_PT_2_47_SHOOT_ID, TAURUS_PT_2_47_SHOOT);
        registerSound(TAURUS_PT_2_47_SWITCH_ID, TAURUS_PT_2_47_SWITCH);
        registerSound(TAURUS_PT_2_47_EMPTY_ID, TAURUS_PT_2_47_EMPTY);

        LOGGER.info("ModSoundEvents initialized");
    }
}
