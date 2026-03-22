package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record FirePacket() implements CustomPayload {

    public static final Id<FirePacket> ID = new Id<>(Identifier.of(MOD_ID, "fire"));

    public static final PacketCodec<RegistryByteBuf, FirePacket> CODEC = PacketCodec.unit(new FirePacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}