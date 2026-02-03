package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record C2SRequestImagePayload(UUID imageId) implements CustomPacketPayload {
    public static final Type<C2SRequestImagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("board", "request_image"));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, C2SRequestImagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUUID(payload.imageId()),
            buf -> new C2SRequestImagePayload(buf.readUUID())
    );
}