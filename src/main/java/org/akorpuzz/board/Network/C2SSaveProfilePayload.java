package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SSaveProfilePayload(String bio, String imageId) implements CustomPacketPayload {
    public static final Type<C2SSaveProfilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("board", "c2s_save_profile"));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, C2SSaveProfilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.bio());
                buf.writeUtf(payload.imageId());
            },
            buf -> new C2SSaveProfilePayload(buf.readUtf(), buf.readUtf())
    );
}