package org.akorpuzz.board.Screens;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.akorpuzz.board.Data.ClientImageCache;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageSelectionScreen extends BoardScreen {

    private final Screen parent;
    private final List<File> allFiles;
    private final Set<File> selectedFiles = new HashSet<>();
    private final Map<File, ResourceLocation> thumbnails = new HashMap<>();
    private final ImageConsumer callback;
    private final boolean multiSelect; // true = NewEntry (hasta 3), false = perfil (1)

    private static final ExecutorService THUMB_LOADER = Executors.newSingleThreadExecutor();

    // Layout
    private static final int HEADER_H  = 48;
    private static final int FOOTER_H  = 40;
    private static final int THUMB_SZ  = 56; // miniatura cuadrada
    private static final int CARD_PAD  = 6;
    private static final int GRID_COLS = 4;
    private static final int GRID_GAP  = 8;

    private int gridX, gridY, gridW, gridH; // área de scroll
    private double scrollOffset = 0;
    private int maxScroll = 0;

    // Estado de hover
    private File hoveredFile = null;

    public ImageSelectionScreen(Screen parent, List<File> files, ImageConsumer callback) {
        super(Component.literal("Select image"));
        this.parent      = parent;
        this.allFiles    = files;
        this.callback    = callback;
        this.multiSelect = (parent instanceof NewEntry);
    }

    @Override
    protected void init() {
        super.init();

        // Área de la cuadrícula de imágenes
        gridX = 10;
        gridY = HEADER_H + 8;
        gridW = this.width - 20;
        gridH = this.height - HEADER_H - FOOTER_H - 16;

        // Calcular maxScroll
        int cardW        = cardWidth();
        int rows         = (int) Math.ceil((double) allFiles.size() / GRID_COLS);
        int totalContentH = rows * (THUMB_SZ + CARD_PAD * 2 + this.font.lineHeight + GRID_GAP) + GRID_GAP;
        maxScroll = Math.max(0, totalContentH - gridH);

        // Botón Confirmar
        this.addRenderableWidget(
            Button.builder(Component.literal("Confirm"), btn -> {
                if (!selectedFiles.isEmpty()) {
                    callback.onImagesSelected(new ArrayList<>(selectedFiles));
                }
                Minecraft.getInstance().setScreen(parent);
            }).bounds(this.width / 2 + 4, this.height - FOOTER_H + 10, 90, 20).build()
        );

        // Botón Cancelar
        this.addRenderableWidget(
            Button.builder(Component.literal("Cancel"), btn ->
                Minecraft.getInstance().setScreen(parent)
            ).bounds(this.width / 2 - 94, this.height - FOOTER_H + 10, 90, 20).build()
        );

        loadThumbnails();
    }

    private int cardWidth() {
        return (gridW - GRID_GAP * (GRID_COLS + 1)) / GRID_COLS;
    }

    // =========================================================================
    // RENDER
    // =========================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // Fondo
        g.fill(0, 0, this.width, this.height, 0xCC050508);
        super.render(g, mouseX, mouseY, partialTicks);

        renderHeader(g);
        renderGrid(g, mouseX, mouseY);
        renderFooter(g);
        renderScrollBar(g);
    }

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, this.width, HEADER_H, 0xEE050505);
        drawDoubleHorizontalLine(g, 0, this.width, HEADER_H);

        g.drawString(this.font, "§lTHE BOARD", 10, 14, 0xFFFFFF, true);
        g.drawCenteredString(this.font, "§fSELECT AN IMAGE", this.width / 2, 14, 0xFFFFFF);

        // Indicador de selección
        int sel     = selectedFiles.size();
        int maxSel  = multiSelect ? 3 : 1;
        String hint = multiSelect
                ? "§7" + sel + " / " + maxSel + " selected"
                : (sel == 1 ? "§7" + sel + " selected" : "§8Click an image to select");
        g.drawString(this.font, hint, this.width - this.font.width(hint.replaceAll("§.", "")) - 10, 18, 0xFFFFFF, false);
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(gridX, gridY, gridX + gridW, gridY + gridH, 0xAA101010);
        drawRectBorder(g, gridX, gridY, gridW, gridH, 0x22FFFFFF);

        if (allFiles.isEmpty()) {
            g.drawCenteredString(this.font,
                    "§8No images on board_images/",
                    gridX + gridW / 2, gridY + gridH / 2, 0x444444);
            return;
        }

        g.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);

        int cW      = cardWidth();
        int rowH    = THUMB_SZ + CARD_PAD * 2 + this.font.lineHeight + GRID_GAP;
        hoveredFile = null;

        for (int i = 0; i < allFiles.size(); i++) {
            File file = allFiles.get(i);
            int col   = i % GRID_COLS;
            int row   = i / GRID_COLS;
            int cX    = gridX + GRID_GAP + col * (cW + GRID_GAP);
            int cY    = gridY + GRID_GAP + row * rowH - (int) scrollOffset;

            // Culling
            if (cY + rowH < gridY || cY > gridY + gridH) continue;

            boolean selected = selectedFiles.contains(file);
            boolean hovered  = mouseX >= cX && mouseX <= cX + cW
                             && mouseY >= cY && mouseY <= cY + rowH - GRID_GAP;
            if (hovered) hoveredFile = file;

            renderImageCard(g, file, cX, cY, cW, selected, hovered);
        }

        g.disableScissor();
    }

    private void renderImageCard(GuiGraphics g, File file,
            int x, int y, int w,
            boolean selected, boolean hovered) {

        int cardH = THUMB_SZ + CARD_PAD * 2 + this.font.lineHeight;

        // Fondo de la tarjeta
        int bg = selected ? 0x55FFAA00 : (hovered ? 0x33FFFFFF : 0x22FFFFFF);
        g.fill(x, y, x + w, y + cardH, bg);

        // Borde — naranja si seleccionado, sutil si hover, invisible si no
        int border = selected ? 0xCCFFAA00 : (hovered ? 0x55FFFFFF : 0x22FFFFFF);
        drawRectBorder(g, x, y, w, cardH, border);

        // Miniatura centrada
        int thumbX = x + (w - THUMB_SZ) / 2;
        int thumbY = y + CARD_PAD;
        ResourceLocation thumb = thumbnails.get(file);
        if (thumb != null) {
            g.blit(thumb, thumbX, thumbY, 0, 0, THUMB_SZ, THUMB_SZ, THUMB_SZ, THUMB_SZ);
        } else {
            // Placeholder mientras carga
            g.fill(thumbX, thumbY, thumbX + THUMB_SZ, thumbY + THUMB_SZ, 0x33FFFFFF);
            g.drawCenteredString(this.font, "§8...",
                    thumbX + THUMB_SZ / 2, thumbY + THUMB_SZ / 2 - 4, 0x444444);
        }

        // Nombre del archivo truncado
        String name  = file.getName();
        int maxChars = (w - 8) / this.font.width("a");
        if (name.length() > maxChars) name = name.substring(0, Math.max(1, maxChars - 1)) + "…";
        int nameColor = selected ? 0xFFFFAA00 : (hovered ? 0xFFFFFFFF : 0xAAAAAAA);
        g.drawCenteredString(this.font, name,
                x + w / 2, y + CARD_PAD + THUMB_SZ + 3, nameColor);

        // Tick de selección en la esquina
        if (selected) {
            g.fill(x + w - 13, y + 2, x + w - 2, y + 13, 0xFFFFAA00);
            g.drawString(this.font, "§0✓", x + w - 11, y + 3, 0x000000, false);
        }
    }

    private void renderFooter(GuiGraphics g) {
        g.fill(0, this.height - FOOTER_H, this.width, this.height, 0xEE050505);
        drawDoubleHorizontalLine(g, 0, this.width, this.height - FOOTER_H);
        // Los botones (Confirmar/Cancelar) los renderiza super.render()
    }

    private void renderScrollBar(GuiGraphics g) {
        if (maxScroll <= 0) return;
        int barH = Math.max(20, (int) ((float) gridH / (gridH + maxScroll) * gridH));
        int barY = gridY + (int) ((float) scrollOffset / maxScroll * (gridH - barH));
        g.fill(gridX + gridW - 3, barY, gridX + gridW - 1, barY + barH, 0x55FFAA00);
    }

    // =========================================================================
    // INPUT
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int cW   = cardWidth();
        int rowH = THUMB_SZ + CARD_PAD * 2 + this.font.lineHeight + GRID_GAP;

        for (int i = 0; i < allFiles.size(); i++) {
            File file = allFiles.get(i);
            int col   = i % GRID_COLS;
            int row   = i / GRID_COLS;
            int cX    = gridX + GRID_GAP + col * (cW + GRID_GAP);
            int cY    = gridY + GRID_GAP + row * rowH - (int) scrollOffset;

            if (mouseX >= cX && mouseX <= cX + cW
                    && mouseY >= cY && mouseY <= cY + rowH - GRID_GAP) {
                if (multiSelect) {
                    if (selectedFiles.contains(file)) selectedFiles.remove(file);
                    else if (selectedFiles.size() < 3) selectedFiles.add(file);
                } else {
                    selectedFiles.clear();
                    selectedFiles.add(file);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= gridX && mouseX <= gridX + gridW
                && mouseY >= gridY && mouseY <= gridY + gridH) {
            scrollOffset -= scrollY * 20;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // =========================================================================
    // CARGA DE MINIATURAS
    // =========================================================================

    private void loadThumbnails() {
        THUMB_LOADER.submit(() -> {
            for (File file : allFiles) {
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                    if (img == null) continue;

                    // Escalar a THUMB_SZ × THUMB_SZ recortando al centro (cover)
                    int srcW = img.getWidth(), srcH = img.getHeight();
                    float scale = Math.max((float) THUMB_SZ / srcW, (float) THUMB_SZ / srcH);
                    int scaledW = (int) (srcW * scale);
                    int scaledH = (int) (srcH * scale);
                    int offsetX = (scaledW - THUMB_SZ) / 2;
                    int offsetY = (scaledH - THUMB_SZ) / 2;

                    java.awt.image.BufferedImage scaled =
                            new java.awt.image.BufferedImage(THUMB_SZ, THUMB_SZ, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g2d = scaled.createGraphics();
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(img, -offsetX, -offsetY, scaledW, scaledH, null);
                    g2d.dispose();

                    final java.awt.image.BufferedImage finalScaled = scaled;
                    Minecraft.getInstance().execute(() -> {
                        try {
                            NativeImage native_ = ClientImageCache.convertToNative(finalScaled);
                            DynamicTexture tex  = new DynamicTexture(native_);
                            ResourceLocation loc = Minecraft.getInstance().getTextureManager()
                                    .register("thumb_" + UUID.randomUUID(), tex);
                            thumbnails.put(file, loc);
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    public interface ImageConsumer {
        void onImagesSelected(List<File> files);
    }
}
