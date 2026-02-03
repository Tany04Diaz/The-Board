package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record C2SRequestEntryPayload(UUID id) implements CustomPacketPayload {
    static ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("board","c2s_request_entry");
    public static final Type<C2SRequestEntryPayload> TYPE =
            new Type<>(resourceLocation);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, C2SRequestEntryPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUUID(payload.id()),
                    buf -> new C2SRequestEntryPayload(buf.readUUID())
            );
}


