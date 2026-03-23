package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record StopAimC2SPayload() implements CustomPayload {

    public static final Id<StopAimC2SPayload> ID = new Id<>(Identifier.of(MOD_ID, "stop_aim"));

    public static final PacketCodec<RegistryByteBuf, StopAimC2SPayload> CODEC =
            PacketCodec.unit(new StopAimC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}