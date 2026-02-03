package org.akorpuzz.board.Data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImageStorage {
    private static final Map<UUID, byte[][]> reassemblyMap = new HashMap<>();

    public static void reciveChunk(UUID id, int index, int total, byte[] data, ServerLevel level){
        reassemblyMap.computeIfAbsent(id, k ->new byte[total][]);
        reassemblyMap.get(id)[index] = data;
        if(isComplete(id)){
            saveImage(id,level);
        }
    }

    private static boolean isComplete(UUID id) {
        byte[][] chunks = reassemblyMap.get(id);
        for (byte[] chunk : chunks){
            if(chunk == null) return false;
        }
        return true;
    }

    private static void saveImage(UUID id, ServerLevel level) {
        byte[][] chunks = reassemblyMap.get(id);
        File dir = new File(level.getServer().getWorldPath(LevelResource.ROOT).toFile(), "board_images");
        if (!dir.exists()) dir.mkdirs();

        File target = new File(dir, id.toString() + ".png");
        try (FileOutputStream fos = new FileOutputStream(target)) {
            for (byte[] chunk : chunks) {
                fos.write(chunk);
            }
            reassemblyMap.remove(id); // Limpiar memoria
            System.out.println("Imagen guardada con éxito: " + target.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
