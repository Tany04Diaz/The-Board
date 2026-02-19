package org.akorpuzz.board.Network;

import net.neoforged.neoforge.network.PacketDistributor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

public class NetworkUtils {
    /**
     * Divide una imagen en trozos de 20KB y los envía al servidor.
     */
    public static void sendImageInChunks(Path path, UUID imageUuid) {
        try {
            byte[] allBytes = Files.readAllBytes(path);
            int chunkSize = 20000;
            int totalChunks = (int) Math.ceil((double) allBytes.length / chunkSize);
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(allBytes.length, start + chunkSize);
                byte[] chunk = Arrays.copyOfRange(allBytes, start, end);

                PacketDistributor.sendToServer(new C2SImageChunkPayload(imageUuid, i, totalChunks, chunk));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}