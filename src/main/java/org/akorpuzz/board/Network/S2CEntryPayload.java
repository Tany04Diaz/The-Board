package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.akorpuzz.board.Data.FeedEntry;

public record S2CEntryPayload(FeedEntry entry) implements CustomPacketPayload {
    public static final Type<S2CEntryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("board", "s2c_entry"));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, S2CEntryPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(payload.entry().day());
                        buf.writeUtf(payload.entry().text(), 250);
                        buf.writeUtf(payload.entry().playerName(), 64);
                        buf.writeUUID(payload.entry().id());
                        // Protección contra imageId nulo
                        String imgId = payload.entry().imageId() != null ? payload.entry().imageId() : "none";
                        buf.writeUtf(imgId);
                    },
                    buf -> new S2CEntryPayload(new FeedEntry(
                            buf.readLong(),
                            buf.readUtf(250),
                            buf.readUtf(64),
                            buf.readUUID(),
                            buf.readUtf()
                    ))
            );
}
