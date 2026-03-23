package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record StartAimC2SPayload() implements CustomPayload {

    public static final Id<StartAimC2SPayload> ID = new Id<>(Identifier.of(MOD_ID, "start_aim"));

    public static final PacketCodec<RegistryByteBuf, StartAimC2SPayload> CODEC =
            PacketCodec.unit(new StartAimC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}