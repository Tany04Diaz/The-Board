package org.akorpuzz.board.Network;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.akorpuzz.board.Data.FeedEntry;

import java.util.List;

public record S2CFeedSyncPayload(List<FeedEntry> entries) implements CustomPacketPayload {
    static ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("board","s2c_feed_sync");
    public static final Type<S2CFeedSyncPayload> TYPE =
            new Type<>(resourceLocation);
    @Override
    public  Type<? extends CustomPacketPayload> type(){
        return TYPE;
    }
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type LIST_TYPE = new TypeToken<List<FeedEntry>>() {}.getType();
    public static final StreamCodec<FriendlyByteBuf, S2CFeedSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.entries().size());
                        for (FeedEntry entry : payload.entries()) {
                            buf.writeLong(entry.day());
                            buf.writeUtf(entry.text(), 32767);
                            buf.writeUtf(entry.playerName(), 64);
                            buf.writeUUID(entry.id());
                            String imgId = entry.imageId() != null ? entry.imageId() : "none";
                            buf.writeUtf(imgId);
                        }
                    },
                    buf -> {
                        int size = buf.readInt();
                        List<FeedEntry> list = new java.util.ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            long day = buf.readLong();
                            String text = buf.readUtf(32767);
                            String playerName = buf.readUtf(64);
                            java.util.UUID id = buf.readUUID();
                            String imageId = buf.readUtf();
                            list.add(new FeedEntry(day, text, playerName, id,imageId));
                        }
                        return new S2CFeedSyncPayload(list);
                    }
            );


}
