package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record ReloadPacket() implements CustomPayload {

    public static final Id<ReloadPacket> ID = new Id<>(Identifier.of(MOD_ID, "reload"));

    public static final PacketCodec<RegistryByteBuf, ReloadPacket> CODEC = PacketCodec.unit(new ReloadPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}