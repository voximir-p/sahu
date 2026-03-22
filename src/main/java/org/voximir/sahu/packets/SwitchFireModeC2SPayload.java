package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record SwitchFireModeC2SPayload() implements CustomPayload {

    public static final Id<SwitchFireModeC2SPayload> ID = new Id<>(Identifier.of(MOD_ID, "switch_fire_mode"));

    public static final PacketCodec<RegistryByteBuf, SwitchFireModeC2SPayload> CODEC =
            PacketCodec.unit(new SwitchFireModeC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
