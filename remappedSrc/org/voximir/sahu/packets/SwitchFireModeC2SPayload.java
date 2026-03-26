package org.voximir.sahu.packets;

import static org.voximir.sahu.Sahu.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SwitchFireModeC2SPayload() implements CustomPacketPayload {

    public static final Type<SwitchFireModeC2SPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "switch_fire_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchFireModeC2SPayload> CODEC =
            StreamCodec.unit(new SwitchFireModeC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
