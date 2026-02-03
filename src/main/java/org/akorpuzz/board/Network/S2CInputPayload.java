package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record S2CInputPayload(UUID playerId, String text) implements CustomPacketPayload {
    static ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("board", "s2c_input");

    public static final CustomPacketPayload.Type<S2CInputPayload> TYPE =
            new CustomPacketPayload.Type<>(resourceLocation);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, S2CInputPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeUUID(payload.playerId());
                buf.writeUtf(payload.text(), 32767);
            }, buf -> new S2CInputPayload(
                    buf.readUUID(),
                    buf.readUtf(32767)
            ));
}