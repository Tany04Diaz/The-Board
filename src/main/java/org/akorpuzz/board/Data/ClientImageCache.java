package org.akorpuzz.board.Data;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientImageCache {
    private static final Map<UUID, byte[][]> downloadMap = new HashMap<>();
    private static final Map<UUID, ResourceLocation> textureCache = new HashMap<>();
    private static final Path CACHE_DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("board_cache");
    private static final ExecutorService imageProcessor = Executors.newSingleThreadExecutor();
    private static final Map<UUID, ImageSize> imageSizes = new HashMap<>();

    public static void receiveChunk(UUID id, int index, int total, byte[] data) {
        downloadMap.computeIfAbsent(id, k -> new byte[total][]);
        downloadMap.get(id)[index] = data;

        if (isComplete(id)) {
            assembleAndRegister(id);
        }
    }

    private static boolean isComplete(UUID id) {
        byte[][] chunks = downloadMap.get(id);
        if (chunks == null) return false;
        for (byte[] chunk : chunks) {
            if (chunk == null) return false;
        }
        return true;
    }
    private static void assembleAndRegister(UUID id) {
        byte[][] chunks = downloadMap.remove(id);
        if (chunks == null) return;

        imageProcessor.submit(() -> {
            try {
                if (!Files.exists(CACHE_DIR)) Files.createDirectories(CACHE_DIR);
                Path outputPath = CACHE_DIR.resolve(id + ".png");

                // 1. Guardar bytes crudos
                try (var os = Files.newOutputStream(outputPath)) {
                    for (byte[] chunk : chunks) {
                        if (chunk != null) os.write(chunk);
                    }
                }
                java.awt.image.BufferedImage originalImg = javax.imageio.ImageIO.read(outputPath.toFile());
                if (originalImg == null) throw new IOException("No es una imagen válida");

// 1. CÁLCULO DE ESCALA
                int targetSize = 1024; // Tamaño máximo
                int width = originalImg.getWidth();
                int height = originalImg.getHeight();

                java.awt.image.BufferedImage finalImg = originalImg;
                int finalW = finalImg.getWidth();
                int finalH = finalImg.getHeight();
                imageSizes.put(id, new ImageSize(finalW, finalH));

                if (width > targetSize || height > targetSize) {
                    double scale = Math.min((double) targetSize / width, (double) targetSize / height);
                    int newW = (int) (width * scale);
                    int newH = (int) (height * scale);

                    // Crear una nueva imagen más pequeña
                    finalImg = new java.awt.image.BufferedImage(newW, newH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g2d = finalImg.createGraphics();

                    // Mejorar calidad del reescalado
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(originalImg, 0, 0, newW, newH, null);
                    g2d.dispose();
                }

// 2. CONVERSIÓN A PNG BYTES
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(finalImg, "png", baos);
                byte[] pngBytes = baos.toByteArray();

// Limpieza de memoria Heap
                originalImg.flush();
                if (finalImg != originalImg) finalImg.flush();
                baos.close();

// 3. REGISTRO EN EL HILO DE RENDER
                Minecraft.getInstance().execute(() -> {
                    try (NativeImage nativeImage = NativeImage.read(new java.io.ByteArrayInputStream(pngBytes))) {
                        DynamicTexture texture = new DynamicTexture(nativeImage);
                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("", "img_" + id.toString().toLowerCase());
                        Minecraft.getInstance().getTextureManager().register(loc, texture);
                        textureCache.put(id, loc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Fallo en la cola de procesamiento: " + e.getMessage());
            }
        });
    }
    private static void processImageFile(UUID id, Path path) {
        try {
            // Conversión ImageIO (Heap)
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(path.toFile());
            if (img == null) return;

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            byte[] bytes = baos.toByteArray();

            img.flush(); // Liberar memoria de la imagen cargada

            // Solo el registro final ocurre en el hilo de Minecraft
            Minecraft.getInstance().execute(() -> {
                try (NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes))) {
                    DynamicTexture texture = new DynamicTexture(nativeImage);
                    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("", "img_" + id.toString().toLowerCase());
                    Minecraft.getInstance().getTextureManager().register(loc, texture);
                    textureCache.put(id, loc);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static ResourceLocation getTexture(String imageId) {
        if (imageId == null || imageId.equals("none")) return null;
        return textureCache.get(UUID.fromString(imageId));
    }
    public static float getDownloadProgress(UUID id) {
        byte[][] chunks = downloadMap.get(id);
        if (chunks == null) return 0f;

        int loaded = 0;
        for (byte[] chunk : chunks) {
            if (chunk != null) loaded++;
        }
        // Retorna un valor entre 0.0 y 1.0
        return (float) loaded / chunks.length;
    }
    public static ImageSize getSize(String idStr) {
        if (idStr == null || idStr.equals("none")) return null;
        try {
            return imageSizes.get(UUID.fromString(idStr));
        } catch (Exception e) {
            return null;
        }
    }
    public static NativeImage convertToNative(java.awt.image.BufferedImage img) {
        NativeImage nativeImage = new NativeImage(img.getWidth(), img.getHeight(), false);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                // Minecraft usa formato ABGR internamente, NativeImage lo maneja así:
                nativeImage.setPixelRGBA(x, y, argb);
            }
        }
        return nativeImage;
    }
}
