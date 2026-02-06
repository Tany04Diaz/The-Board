package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRequestFeedPayload() implements CustomPacketPayload {
    public static final Type<C2SRequestFeedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("board", "c2s_request_feed"));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, C2SRequestFeedPayload> STREAM_CODEC =
            StreamCodec.unit(new C2SRequestFeedPayload());
}