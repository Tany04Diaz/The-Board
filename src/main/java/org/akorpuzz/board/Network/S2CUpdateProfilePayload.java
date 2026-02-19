package org.akorpuzz.board.Network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record S2CUpdateProfilePayload(UUID uuid, String name, String bio, String imageId) implements CustomPacketPayload {
    public static final Type<S2CUpdateProfilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("board", "s2c_update_profile"));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, S2CUpdateProfilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.uuid());
                buf.writeUtf(payload.name());
                buf.writeUtf(payload.bio());
                buf.writeUtf(payload.imageId());
            },
            buf -> new S2CUpdateProfilePayload(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf())
    );
}