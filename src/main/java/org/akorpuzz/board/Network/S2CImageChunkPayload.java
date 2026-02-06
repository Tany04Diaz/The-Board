package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record S2CImageChunkPayload(UUID imageId, int chunkIndex, int totalChunks, byte[] data) implements CustomPacketPayload {
    // ID único para BAJADA
    public static final Type<S2CImageChunkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("board", "s2c_image_chunk"));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, S2CImageChunkPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.imageId());
                buf.writeInt(payload.chunkIndex());
                buf.writeInt(payload.totalChunks());
                buf.writeByteArray(payload.data());
            },
            buf -> new S2CImageChunkPayload(buf.readUUID(), buf.readInt(), buf.readInt(), buf.readByteArray())
    );
}