package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record C2SImageChunkPayload(UUID imageId, int chunkIndex, int totalChunks, byte[] data) implements CustomPacketPayload {
    // ID único para SUBIDA
    public static final Type<C2SImageChunkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("board", "c2s_image_chunk"));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, C2SImageChunkPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.imageId());
                        buf.writeInt(payload.chunkIndex());
                        buf.writeInt(payload.totalChunks());
                        buf.writeByteArray(payload.data());
                    },
                    buf -> new C2SImageChunkPayload(buf.readUUID(), buf.readInt(), buf.readInt(), buf.readByteArray())
            );
}