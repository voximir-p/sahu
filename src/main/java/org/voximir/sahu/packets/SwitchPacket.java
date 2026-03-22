package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record SwitchPacket() implements CustomPayload {

    public static final Id<SwitchPacket> ID = new Id<>(Identifier.of(MOD_ID, "switch"));

    public static final PacketCodec<RegistryByteBuf, SwitchPacket> CODEC = PacketCodec.unit(new SwitchPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}