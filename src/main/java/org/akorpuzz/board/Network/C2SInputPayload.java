package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record C2SInputPayload(long day, String text, String playerName, UUID id, String imageId)implements CustomPacketPayload {
    static ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("board","c2s_input");
    public static final Type<C2SInputPayload> TYPE =
            new Type<>(resourceLocation);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static final StreamCodec<FriendlyByteBuf,C2SInputPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf,payload) -> {
                        buf.writeLong(payload.day());
                        buf.writeUtf(payload.text(), 32767);
                        buf.writeUtf(payload.playerName(),64);
                        buf.writeUUID(payload.id());
                        buf.writeUtf(payload.imageId);
                    },
                    buf -> new C2SInputPayload(
                            buf.readLong(),
                            buf.readUtf(32767),
                            buf.readUtf(64),
                            buf.readUUID(),
                            buf.readUtf()
                    )
            );
}
