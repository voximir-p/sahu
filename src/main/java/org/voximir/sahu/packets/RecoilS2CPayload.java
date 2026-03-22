package org.voximir.sahu.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public record RecoilS2CPayload(float pitchKick, float yawKick, int duration) implements CustomPayload {

    public static final Id<RecoilS2CPayload> ID = new Id<>(Identifier.of(MOD_ID, "recoil"));

    public static final PacketCodec<RegistryByteBuf, RecoilS2CPayload> CODEC = new PacketCodec<>() {
        @Override
        public RecoilS2CPayload decode(RegistryByteBuf buf) {
            float pitchKick = buf.readFloat();
            float yawKick = buf.readFloat();
            int duration = buf.readVarInt();
            return new RecoilS2CPayload(pitchKick, yawKick, duration);
        }

        @Override
        public void encode(RegistryByteBuf buf, RecoilS2CPayload payload) {
            buf.writeFloat(payload.pitchKick());
            buf.writeFloat(payload.yawKick());
            buf.writeVarInt(payload.duration());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

