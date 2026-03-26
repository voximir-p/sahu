package org.voximir.sahu.packets;

import static org.voximir.sahu.Sahu.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StartAimC2SPayload() implements CustomPacketPayload {

    public static final Type<StartAimC2SPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "start_aim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartAimC2SPayload> CODEC =
            StreamCodec.unit(new StartAimC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}