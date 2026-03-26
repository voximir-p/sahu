package org.voximir.sahu.packets;

import static org.voximir.sahu.Sahu.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RecoilS2CPayload(float pitchKick, float yawKick, int duration) implements CustomPacketPayload {

    public static final Type<RecoilS2CPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "recoil"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecoilS2CPayload> CODEC = new StreamCodec<>() {
        @Override
        public RecoilS2CPayload decode(RegistryFriendlyByteBuf buf) {
            float pitchKick = buf.readFloat();
            float yawKick = buf.readFloat();
            int duration = buf.readVarInt();
            return new RecoilS2CPayload(pitchKick, yawKick, duration);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, RecoilS2CPayload payload) {
            buf.writeFloat(payload.pitchKick());
            buf.writeFloat(payload.yawKick());
            buf.writeVarInt(payload.duration());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

