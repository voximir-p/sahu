package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record ReloadC2SPayload() implements CustomPayload {

    public static final Id<ReloadC2SPayload> ID = new Id<>(Identifier.of(MOD_ID, "reload"));

    public static final PacketCodec<RegistryByteBuf, ReloadC2SPayload> CODEC =
            PacketCodec.unit(new ReloadC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
