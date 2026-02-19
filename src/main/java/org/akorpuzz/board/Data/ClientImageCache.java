package org.akorpuzz.board.Data;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, byte[][]> downloadMap = new HashMap<>();
    private static final Map<UUID, ResourceLocation> textureCache = new HashMap<>();
    private static final Path CACHE_DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("board_cache");
    private static final ExecutorService imageProcessor = Executors.newSingleThreadExecutor();
    private static final Map<UUID, ImageSize> imageSizes = new HashMap<>();

    // -------------------------------------------------------------------------
    // Recepción de chunks desde el servidor
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Ensamblado, escalado y registro de textura
    // -------------------------------------------------------------------------

    private static void assembleAndRegister(UUID id) {
        byte[][] chunks = downloadMap.remove(id);
        if (chunks == null) return;

        imageProcessor.submit(() -> {
            try {
                if (!Files.exists(CACHE_DIR)) Files.createDirectories(CACHE_DIR);
                Path outputPath = CACHE_DIR.resolve(id + ".png");

                // 1. Escribir bytes en disco
                try (var os = Files.newOutputStream(outputPath)) {
                    for (byte[] chunk : chunks) {
                        if (chunk != null) os.write(chunk);
                    }
                }

                java.awt.image.BufferedImage originalImg = javax.imageio.ImageIO.read(outputPath.toFile());
                if (originalImg == null) throw new IOException("No es una imagen válida: " + id);

                // 2. Escalar si supera el tamaño máximo
                int targetSize = 1024;
                int width = originalImg.getWidth();
                int height = originalImg.getHeight();

                java.awt.image.BufferedImage finalImg = originalImg;

                if (width > targetSize || height > targetSize) {
                    double scale = Math.min((double) targetSize / width, (double) targetSize / height);
                    int newW = (int) (width * scale);
                    int newH = (int) (height * scale);

                    finalImg = new java.awt.image.BufferedImage(newW, newH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g2d = finalImg.createGraphics();
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(originalImg, 0, 0, newW, newH, null);
                    g2d.dispose();
                }

                // 3. Guardar dimensiones finales ANTES de convertir a PNG
                final int finalW = finalImg.getWidth();
                final int finalH = finalImg.getHeight();

                // 4. Convertir a PNG bytes
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(finalImg, "png", baos);
                byte[] pngBytes = baos.toByteArray();

                // Liberar memoria
                originalImg.flush();
                if (finalImg != originalImg) finalImg.flush();
                baos.close();

                // 5. Registrar textura en el hilo de render
                Minecraft.getInstance().execute(() -> {
                    try (NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pngBytes))) {
                        DynamicTexture texture = new DynamicTexture(nativeImage);
                        // CORREGIDO: namespace "board" en lugar de ""
                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("board", "img_" + id.toString().toLowerCase());
                        Minecraft.getInstance().getTextureManager().register(loc, texture);
                        textureCache.put(id, loc);
                        // Guardar tamaño después de registrar exitosamente
                        imageSizes.put(id, new ImageSize(finalW, finalH));
                        LOGGER.debug("Textura registrada: {} ({}x{})", loc, finalW, finalH);
                    } catch (Exception e) {
                        LOGGER.error("Error registrando textura {}", id, e);
                    }
                });

            } catch (Exception e) {
                LOGGER.error("Fallo en la cola de procesamiento de imagen {}", id, e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Consulta de texturas y tamaños
    // -------------------------------------------------------------------------

    public static ResourceLocation getTexture(String imageId) {
        if (imageId == null || imageId.equals("none")) return null;
        try {
            return textureCache.get(UUID.fromString(imageId));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static float getDownloadProgress(UUID id) {
        byte[][] chunks = downloadMap.get(id);
        if (chunks == null) return 0f;

        int loaded = 0;
        for (byte[] chunk : chunks) {
            if (chunk != null) loaded++;
        }
        return (float) loaded / chunks.length;
    }

    public static ImageSize getSize(String idStr) {
        if (idStr == null || idStr.equals("none")) return null;
        try {
            return imageSizes.get(UUID.fromString(idStr));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Conversión manual BufferedImage → NativeImage
    // CORREGIDO: BufferedImage usa ARGB, NativeImage espera ABGR
    // -------------------------------------------------------------------------

    public static NativeImage convertToNative(java.awt.image.BufferedImage img) {
        NativeImage nativeImage = new NativeImage(img.getWidth(), img.getHeight(), false);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                // Descomponer ARGB y recomponer como ABGR (formato interno de NativeImage)
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >>  8) & 0xFF;
                int b =  argb        & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        return nativeImage;
    }
}
