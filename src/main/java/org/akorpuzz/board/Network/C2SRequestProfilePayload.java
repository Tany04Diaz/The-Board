package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record C2SRequestProfilePayload(UUID targetUUID) implements CustomPacketPayload {
    public static final Type<C2SRequestProfilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("board", "c2s_request_profile"));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, C2SRequestProfilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUUID(payload.targetUUID()),
            buf -> new C2SRequestProfilePayload(buf.readUUID())
    );
}