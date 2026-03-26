package org.voximir.sahu.packets;

import static org.voximir.sahu.Sahu.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StopFireC2SPayload() implements CustomPacketPayload {

    public static final Type<StopFireC2SPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "stop_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopFireC2SPayload> CODEC =
            StreamCodec.unit(new StopFireC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

