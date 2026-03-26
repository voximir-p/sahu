package org.voximir.sahu;

import org.voximir.sahu.items.GunSounds;

import static org.voximir.sahu.Sahu.LOGGER;
import static org.voximir.sahu.Sahu.MOD_ID;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {

    // ── Taurus PT 24/7 ──────────────────────────────────────────────────

    public static final SoundEvent TAURUS_PT_24_7_SHOOT  = register("taurus_pt_24_7_shoot");
    public static final SoundEvent TAURUS_PT_24_7_EMPTY  = register("taurus_pt_24_7_empty");
    public static final SoundEvent TAURUS_PT_24_7_SWITCH = register("taurus_pt_24_7_switch");
    public static final SoundEvent TAURUS_PT_24_7_MAGIN  = register("taurus_pt_24_7_magin");
    public static final SoundEvent TAURUS_PT_24_7_RELOAD = register("taurus_pt_24_7_reload");

    public static final GunSounds TAURUS_PT_24_7_SOUNDS = new GunSounds(
            TAURUS_PT_24_7_SHOOT,
            TAURUS_PT_24_7_EMPTY,
            TAURUS_PT_24_7_SWITCH,
            TAURUS_PT_24_7_MAGIN,
            TAURUS_PT_24_7_RELOAD
    );

    // ── Registration helper ──────────────────────────────────────────────

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        return event;
    }

    public static void initialize() {
        LOGGER.info("ModSoundEvents initialized");
    }
}
