package org.akorpuzz.board.Network;

import org.akorpuzz.board.Data.ClientImageCache;
import  org.akorpuzz.board.Data.ImageStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.akorpuzz.board.Data.FeedEntry;
import org.akorpuzz.board.Data.FeedStorage;
import org.akorpuzz.board.Screens.FeedScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = "board", bus = EventBusSubscriber.Bus.MOD)
public class Network {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("board");
        // Registrar paquete de Cliente a Servidor publicacion
        registrar.playToServer(
                C2SInputPayload.TYPE,
                C2SInputPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            // Reconstruir FeedEntry desde el payload
                            FeedEntry entry = new FeedEntry(
                                    payload.day(),
                                    payload.text(),
                                    payload.playerName(),
                                    payload.id(),
                                    payload.imageId()
                            );

                            // Guardar en JSON
                            List<FeedEntry> entries;
                            try {
                                entries = FeedStorage.load(player.serverLevel());
                            } catch (IOException e) {
                                e.printStackTrace();
                                entries = new ArrayList<>();
                            }
                            entries.add(entry);
                            FeedStorage.save(player.serverLevel(), entries);
                        }
                    });
                }
        );
        //Enviar trozos de imagen
        registrar.playToServer(
                C2SImageChunkPayload.TYPE,
                C2SImageChunkPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            ImageStorage.reciveChunk(
                                    payload.imageId(),
                                    payload.chunkIndex(),
                                    payload.totalChunks(),
                                    payload.data(),
                                    player.serverLevel()
                            );
                        }
                    });
                }
        );
        //Enviar feed al cliente
        registrar.playToServer(
                C2SRequestEntryPayload.TYPE,
                C2SRequestEntryPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            try {
                                List<FeedEntry> entries = FeedStorage.load(player.serverLevel());
                                List<FeedEntry> entriesParaEnviar = new ArrayList<>(entries);
                                Collections.reverse(entriesParaEnviar);
                                PacketDistributor.sendToPlayer(player, new S2CFeedSyncPayload(entriesParaEnviar));
                            } catch (IOException e) {
                                e.printStackTrace();

                            }
                        }
                    });
                }
        );
        //enviar imagen al cliente
        registrar.playToServer(
                C2SRequestImagePayload.TYPE,
                C2SRequestImagePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
                        java.util.UUID imageId = payload.imageId();

                        // 1. Localizar la imagen en la carpeta del mundo
                        java.io.File dir = new java.io.File(player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile(), "board_images");
                        java.io.File file = new java.io.File(dir, imageId.toString() + ".png");

                        if (file.exists()) {
                            try {
                                byte[] allBytes = java.nio.file.Files.readAllBytes(file.toPath());
                                int chunkSize = 20000; // Tamaño seguro para paquetes de red
                                int totalChunks = (int) Math.ceil((double) allBytes.length / chunkSize);

                                // 2. Enviar la imagen al cliente en trozos
                                for (int i = 0; i < totalChunks; i++) {
                                    int start = i * chunkSize;
                                    int end = Math.min(allBytes.length, start + chunkSize);
                                    byte[] chunkData = java.util.Arrays.copyOfRange(allBytes, start, end);

                                    context.reply(new S2CImageChunkPayload(imageId, i, totalChunks, chunkData));
                                }
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
        );
// Handler para mensajes individuales (debug)
        registrar.playToClient(
                S2CInputPayload.TYPE,
                S2CInputPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() != null) {
                            System.out.println("El jugador " + payload.playerId() + " envió: " + payload.text());
                        }
                    });
                }
        );

// Handler para sincronizar el feed completo
        registrar.playToClient(
                S2CFeedSyncPayload.TYPE,
                S2CFeedSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft mc = Minecraft.getInstance();
                        // Esto asegura que la pantalla se abra con los datos frescos del servidor
                        mc.setScreen(new FeedScreen(mc.player.getName().getString(), payload.entries()));
                    });
                }
        );
        //handler para la imagen
        registrar.playToClient(
                S2CImageChunkPayload.TYPE,
                S2CImageChunkPayload.STREAM_CODEC,
                (payload,context) -> {
                    context.enqueueWork(()->{
                        ClientImageCache.receiveChunk(
                                payload.imageId(),
                                payload.chunkIndex(),
                                payload.totalChunks(),
                                payload.data()
                        );
                    });
                }
        );


    }
}
