package org.akorpuzz.board.Data;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Network.S2CImageChunkPayload;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImageStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, byte[][]> reassemblyMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Recepción de chunks del cliente y ensamblado
    // -------------------------------------------------------------------------

    public static void handleImageChunk(UUID id, int index, int total, byte[] data, ServerLevel level) {
        reassemblyMap.computeIfAbsent(id, k -> new byte[total][]);
        reassemblyMap.get(id)[index] = data;

        if (isComplete(id)) {
            saveImage(id, level);
        }
    }

    private static boolean isComplete(UUID id) {
        byte[][] chunks = reassemblyMap.get(id);
        if (chunks == null) return false;
        for (byte[] chunk : chunks) {
            if (chunk == null) return false;
        }
        return true;
    }

    private static void saveImage(UUID id, ServerLevel level) {
        byte[][] chunks = reassemblyMap.remove(id);
        if (chunks == null) return;

        // CORREGIDO: usar Path en lugar de File
        Path dir = getImagesDir(level);
        Path target = dir.resolve(id + ".png");

        try {
            Files.createDirectories(dir);
            // Calcular tamaño total para escribir de una sola vez
            int totalSize = 0;
            for (byte[] chunk : chunks) totalSize += chunk.length;

            byte[] allBytes = new byte[totalSize];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, allBytes, offset, chunk.length);
                offset += chunk.length;
            }

            Files.write(target, allBytes);
            LOGGER.info("Imagen guardada: {}", target.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Error guardando imagen {}", id, e);
        }
    }

    // -------------------------------------------------------------------------
    // Envío de imagen al cliente en chunks
    // -------------------------------------------------------------------------

    public static void sendImageToClient(UUID imageId, ServerPlayer player) {
        Path file = getImagesDir(player.serverLevel()).resolve(imageId + ".png");

        if (!Files.exists(file)) {
            LOGGER.warn("Imagen solicitada no encontrada: {}", imageId);
            return;
        }

        try {
            byte[] allBytes = Files.readAllBytes(file);
            int chunkSize = 20000; // 20KB por chunk
            int totalChunks = (int) Math.ceil((double) allBytes.length / chunkSize);

            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(allBytes.length, start + chunkSize);
                byte[] chunk = Arrays.copyOfRange(allBytes, start, end);
                PacketDistributor.sendToPlayer(player, new S2CImageChunkPayload(imageId, i, totalChunks, chunk));
            }
        } catch (IOException e) {
            LOGGER.error("Error enviando imagen {} al cliente", imageId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private static Path getImagesDir(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("board_images");
    }
}
