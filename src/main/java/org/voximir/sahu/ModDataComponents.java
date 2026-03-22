package org.voximir.sahu;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.LOGGER;
import static org.voximir.sahu.Sahu.MOD_ID;

public class ModDataComponents {

    public static final ComponentType<Integer> AMMO = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "ammo"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build()
    );

    public static final ComponentType<FireMode> FIRE_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "fire_mode"),
            ComponentType.<FireMode>builder()
                    .codec(Codec.INT.xmap(FireMode::fromId, FireMode::getId))
                    .packetCodec(PacketCodecs.VAR_INT.xmap(FireMode::fromId, FireMode::getId))
                    .build()
    );

    public static final ComponentType<Boolean> RELOADING = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "reloading"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOLEAN)
                    .build()
    );

    public static void initialize() {
        LOGGER.info("ModDataComponents initialized");
    }
}
