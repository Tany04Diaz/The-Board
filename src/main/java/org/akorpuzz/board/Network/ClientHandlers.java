package org.akorpuzz.board.Network;

import net.minecraft.client.Minecraft;
import org.akorpuzz.board.Data.ClientFeedCache;
import org.akorpuzz.board.Data.ClientImageCache;
import org.akorpuzz.board.Data.ClientProfileCache;
import org.akorpuzz.board.Screens.FeedScreen;
import org.akorpuzz.board.Screens.HubScreen;
import org.akorpuzz.board.Screens.UserProfileScreen;

public class ClientHandlers {

    public static void handleFeedSync(S2CFeedSyncPayload payload) {
        ClientFeedCache.setEntries(payload.entries());
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new FeedScreen(
                mc.player.getName().getString(),
                ClientFeedCache.getEntries(),
                ClientProfileCache.getLocalProfileImageId()
        )));
    }

    public static void handleImageChunk(S2CImageChunkPayload payload) {
        ClientImageCache.receiveChunk(
                payload.imageId(), payload.chunkIndex(),
                payload.totalChunks(), payload.data());
    }

    public static void handleProfileUpdate(S2CUpdateProfilePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null && payload.uuid().equals(mc.player.getUUID())) {
                ClientProfileCache.setLocalProfile(payload);
            }
            mc.setScreen(new UserProfileScreen(payload, ClientFeedCache.getEntries()));
        });
    }

    /** Recibe la lista de jugadores online y la pasa al HubScreen activo */
    public static void handlePlayerList(S2CPlayerListPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof HubScreen hub) {
                hub.setPlayerList(payload.players());
            }
        });
    }
}
