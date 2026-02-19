package org.akorpuzz.board.Network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.akorpuzz.board.Data.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = "board", bus = EventBusSubscriber.Bus.MOD)
public class Network {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("board").versioned("1");

        // =====================================================================
        // C2S — Cliente → Servidor
        // =====================================================================

        registrar.playToServer(C2SInputPayload.TYPE, C2SInputPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                try {
                    List<FeedEntry> entries = FeedStorage.load(player.serverLevel());
                    entries.add(new FeedEntry(payload.day(), payload.text(),
                            payload.playerName(), payload.id(), payload.imageId()));
                    FeedStorage.save(player.serverLevel(), entries);
                    sendUpdatedFeed(player);
                } catch (IOException e) { e.printStackTrace(); }
            });
        });

        registrar.playToServer(C2SRequestFeedPayload.TYPE, C2SRequestFeedPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> sendUpdatedFeed((ServerPlayer) context.player()));
        });

        registrar.playToServer(C2SRequestImagePayload.TYPE, C2SRequestImagePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                try {
                    ImageStorage.sendImageToClient(payload.imageId(), (ServerPlayer) context.player());
                } catch (Exception e) { e.printStackTrace(); }
            });
        });

        registrar.playToServer(C2SRequestProfilePayload.TYPE, C2SRequestProfilePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                ProfileStorage.sendProfileSync(player, payload.targetUUID());
            });
        });

        registrar.playToServer(C2SRequestEntryPayload.TYPE, C2SRequestEntryPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> sendUpdatedFeed((ServerPlayer) context.player()));
        });

        registrar.playToServer(C2SImageChunkPayload.TYPE, C2SImageChunkPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                ImageStorage.handleImageChunk(payload.imageId(), payload.chunkIndex(),
                        payload.totalChunks(), payload.data(), player.serverLevel());
            });
        });

        registrar.playToServer(C2SSaveProfilePayload.TYPE, C2SSaveProfilePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                UserProfile existing = ProfileStorage.load(
                        player.serverLevel(), player.getUUID(), player.getName().getString());
                UserProfile updated = new UserProfile(
                        existing.playerUUID(), existing.playerName(),
                        payload.bio(), payload.imageId());
                ProfileStorage.save(player.serverLevel(), updated);
                ProfileStorage.sendProfileSync(player, player.getUUID());
            });
        });

        // Lista de jugadores: incluye imageId del perfil de cada uno
        registrar.playToServer(C2SRequestPlayerListPayload.TYPE, C2SRequestPlayerListPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer requester = (ServerPlayer) context.player();
                List<S2CPlayerListPayload.PlayerEntry> players =
                        requester.getServer().getPlayerList().getPlayers().stream()
                                .map(p -> {
                                    // Leer imageId del perfil guardado en disco
                                    UserProfile profile = ProfileStorage.load(
                                            p.serverLevel(), p.getUUID(), p.getName().getString());
                                    String imageId = profile.profileImageId() != null
                                            ? profile.profileImageId() : "none";
                                    return new S2CPlayerListPayload.PlayerEntry(
                                            p.getUUID(), p.getName().getString(), imageId);
                                })
                                .collect(Collectors.toList());
                PacketDistributor.sendToPlayer(requester, new S2CPlayerListPayload(players));
            });
        });

        // =====================================================================
        // S2C — Servidor → Cliente
        // =====================================================================

        registrar.playToClient(S2CFeedSyncPayload.TYPE, S2CFeedSyncPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientHandlers.handleFeedSync(payload));
        });

        registrar.playToClient(S2CUpdateProfilePayload.TYPE, S2CUpdateProfilePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientHandlers.handleProfileUpdate(payload));
        });

        registrar.playToClient(S2CImageChunkPayload.TYPE, S2CImageChunkPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientHandlers.handleImageChunk(payload));
        });

        registrar.playToClient(S2CPlayerListPayload.TYPE, S2CPlayerListPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientHandlers.handlePlayerList(payload));
        });
    }

    private static void sendUpdatedFeed(ServerPlayer player) {
        try {
            List<FeedEntry> entries = new ArrayList<>(FeedStorage.load(player.serverLevel()));
            Collections.reverse(entries);
            PacketDistributor.sendToPlayer(player, new S2CFeedSyncPayload(entries));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
