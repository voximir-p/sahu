package org.voximir.sahu.packets;

import static org.voximir.sahu.Sahu.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StartFireC2SPayload() implements CustomPacketPayload {

    public static final Type<StartFireC2SPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "start_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartFireC2SPayload> CODEC =
            StreamCodec.unit(new StartFireC2SPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

