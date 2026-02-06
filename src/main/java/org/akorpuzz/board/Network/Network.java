package org.akorpuzz.board.Network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.akorpuzz.board.Data.FeedEntry;
import org.akorpuzz.board.Data.FeedStorage;
import org.akorpuzz.board.Data.ImageStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = "board", bus = EventBusSubscriber.Bus.MOD)
public class Network {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("board").versioned("1");

        // --- CLIENTE A SERVIDOR (C2S)
        // 1. Enviar el texto de la publicación
        registrar.playToServer(C2SInputPayload.TYPE, C2SInputPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                try {
                    List<FeedEntry> entries = FeedStorage.load(player.serverLevel());
                    entries.add(new FeedEntry(payload.day(), payload.text(), payload.playerName(), payload.id(), payload.imageId()));
                    FeedStorage.save(player.serverLevel(), entries);

                    // Responder enviando el feed actualizado para que la pantalla se abra sola
                    List<FeedEntry> syncList = new ArrayList<>(entries);
                    Collections.reverse(syncList);
                    PacketDistributor.sendToPlayer(player, new S2CFeedSyncPayload(syncList));
                } catch (IOException e) { e.printStackTrace(); }
            });
        });

        // 2. Subir trozos de imagen
        registrar.playToServer(C2SImageChunkPayload.TYPE, C2SImageChunkPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                ImageStorage.reciveChunk(payload.imageId(), payload.chunkIndex(), payload.totalChunks(), payload.data(), player.serverLevel());
            });
        });

        // 3. Pedir una entrada específica
        registrar.playToServer(C2SRequestEntryPayload.TYPE, C2SRequestEntryPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                try {
                    FeedEntry entry = FeedStorage.getEntryById(player.serverLevel(), payload.id());
                    if (entry != null) {
                        PacketDistributor.sendToPlayer(player, new S2CEntryPayload(entry));
                    }
                } catch (IOException e) { e.printStackTrace(); }
            });
        });
        // 4. Pedir una imagen
        registrar.playToServer(C2SRequestImagePayload.TYPE, C2SRequestImagePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                ImageStorage.sendImageToClient(payload.imageId(), player);
            });
        });
        registrar.playToServer(
                C2SRequestFeedPayload.TYPE,
                C2SRequestFeedPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        sendUpdatedFeed(player);
                    });
                }
        );
        // --- SERVIDOR A CLIENTE (S2C)
        // Sincronizar toda la lista
        registrar.playToClient(S2CFeedSyncPayload.TYPE, S2CFeedSyncPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientHandlers.handleFeedSync(payload));
        });
        // Enviar una sola entrada
        registrar.playToClient(S2CEntryPayload.TYPE, S2CEntryPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> { /* Lógica para manejar una sola entrada si fuera necesario */ });
        });
        // Enviar trozos de imagen al cliente
        registrar.playToClient(S2CImageChunkPayload.TYPE, S2CImageChunkPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientHandlers.handleImageChunk(payload));
        });
    }
    private static void sendUpdatedFeed(ServerPlayer player) {
        try {
            List<FeedEntry> entries = new ArrayList<>(FeedStorage.load(player.serverLevel()));
            Collections.reverse(entries);
            PacketDistributor.sendToPlayer(player, new S2CFeedSyncPayload(entries));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}