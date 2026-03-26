package org.voximir.sahu;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

import static org.voximir.sahu.Sahu.LOGGER;
import static org.voximir.sahu.Sahu.MOD_ID;

public class ModDataComponents {

    public static final DataComponentType<Integer> AMMO = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "ammo"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .ignoreSwapAnimation()
                    .build()
    );

    public static final DataComponentType<FireMode> FIRE_MODE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "fire_mode"),
            DataComponentType.<FireMode>builder()
                    .persistent(Codec.INT.xmap(FireMode::fromId, FireMode::getId))
                    .networkSynchronized(ByteBufCodecs.VAR_INT.map(FireMode::fromId, FireMode::getId))
                    .ignoreSwapAnimation()
                    .build()
    );

    public static final DataComponentType<Boolean> RELOADING = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "reloading"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .ignoreSwapAnimation()
                    .build()
    );

    public static final DataComponentType<Boolean> AIMING = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "aiming"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .ignoreSwapAnimation()
                    .build()
    );

    public static void initialize() {
        LOGGER.info("ModDataComponents initialized");
    }
}
