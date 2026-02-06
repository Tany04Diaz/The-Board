package org.akorpuzz.board.Network;

import net.minecraft.client.Minecraft;
import org.akorpuzz.board.Data.ClientImageCache;
import org.akorpuzz.board.Screens.FeedScreen;

public class ClientHandlers {

    public static void handleFeedSync(S2CFeedSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new FeedScreen(mc.player.getName().getString(), payload.entries()));
    }
    public static void handleImageChunk(S2CImageChunkPayload payload) {
        ClientImageCache.receiveChunk(
                payload.imageId(),
                payload.chunkIndex(),
                payload.totalChunks(),
                payload.data()
        );
    }
}