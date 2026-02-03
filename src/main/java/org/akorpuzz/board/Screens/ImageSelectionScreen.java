package org.akorpuzz.board.Screens;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.akorpuzz.board.Data.ClientImageCache;

import java.io.File;
import java.util.*;

public class ImageSelectionScreen extends Screen {
    private final NewEntry parent;
    private final List<File> allFiles;
    private final Set<File> selectedFiles = new HashSet<>();
    private final Map<File, ResourceLocation> thumbnails = new HashMap<>();
    private double scrollAmount = 0;
    private static final int MAX_SELECTION = 3;

    public ImageSelectionScreen(NewEntry parent, List<File> files, List<File> alreadySelected) {
        super(Component.literal("Seleccionar Imágenes"));
        this.parent = parent;
        this.allFiles = files;
        this.selectedFiles.addAll(alreadySelected);
    }

    @Override
    protected void init() {
        // Botón Confirmar: Envía la lista de vuelta a NewEntry
        this.addRenderableWidget(Button.builder(Component.literal("Confirmar Selección"), b -> {
            this.parent.setSelectedImages(new ArrayList<>(selectedFiles));
            Minecraft.getInstance().setScreen(this.parent);
        }).bounds(this.width / 2 - 105, this.height - 40, 100, 20).build());

        // Botón Volver: Cancela y regresa sin guardar cambios
        this.addRenderableWidget(Button.builder(Component.literal("Volver"), b -> {
            Minecraft.getInstance().setScreen(this.parent);
        }).bounds(this.width / 2 + 5, this.height - 40, 100, 20).build());

        // Cargar miniaturas de forma asíncrona para no bloquear el hilo principal
        loadThumbnails();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int listX = this.width / 2 - 100;
        int listY = 40;
        int listW = 200;
        int listH = 160;

        // Dibujar el contenedor de la lista
        guiGraphics.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2, 0xFF808080);
        guiGraphics.fill(listX, listY, listX + listW, listY + listH, 0xFF303030);

        // Recorte para que los elementos no se salgan del cuadro al hacer scroll
        guiGraphics.enableScissor(listX, listY, listX + listW, listY + listH);

        int entryY = listY - (int)scrollAmount;
        for (File file : allFiles) {
            if (entryY + 25 > listY && entryY < listY + listH) {
                renderEntry(guiGraphics, file, listX, entryY, mouseX, mouseY);
            }
            entryY += 25;
        }

        guiGraphics.disableScissor();

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Seleccionadas: " + selectedFiles.size() + "/" + MAX_SELECTION, this.width / 2, 25, 0xAAAAAA);

    }

    private void renderEntry(GuiGraphics g, File file, int x, int y, int mx, int my) {
        boolean isSelected = selectedFiles.contains(file);
        boolean isHovered = mx >= x && mx <= x + 200 && my >= y && my <= y + 25;

        // Fondo al pasar el ratón
        if (isHovered) g.fill(x, y, x + 200, y + 25, 0x33FFFFFF);

        // Casilla de selección (Checkbox)
        g.fill(x + 2, y + 2, x + 18, y + 18, isSelected ? 0xFF008200: 0xFF505050);
        if (isSelected) g.drawString(this.font, "✔", x + 6, y + 5, 0xFFFFFF);

        // Miniatura cargada dinámicamente
        ResourceLocation tex = thumbnails.get(file);
        if (tex != null) {
            g.blit(tex, x + 22, y + 2, 0, 0, 20, 20, 20, 20);
        } else {
            g.fill(x + 22, y + 2, x + 42, y + 22, 0x22FFFFFF); // Placeholder mientras carga
        }

        // Nombre del archivo
        String name = file.getName();
        if (name.length() > 22) name = name.substring(0, 19) + "...";
        g.drawString(this.font, name, x + 48, y + 8, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = this.width / 2 - 100;
        int listY = 40;
        int entryY = listY - (int)scrollAmount;

        for (File file : allFiles) {
            // Detectar clic en la fila del archivo
            if (mouseX >= listX && mouseX <= listX + 200 && mouseY >= entryY && mouseY <= entryY + 25) {
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file);
                } else if (selectedFiles.size() < MAX_SELECTION) {
                    selectedFiles.add(file);
                }
                return true;
            }
            entryY += 25;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollAmount -= scrollY * 12; // Velocidad de scroll
        int maxScroll = Math.max(0, (allFiles.size() * 25) - 160);
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, maxScroll));
        return true;
    }

    private void loadThumbnails() {
        new Thread(() -> {
            for (File file : allFiles) {
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                    if (img != null) {
                        Minecraft.getInstance().execute(() -> {
                            // Uso correcto de NativeImage y DynamicTexture
                            NativeImage nativeImage = ClientImageCache.convertToNative(img);
                            DynamicTexture texture = new DynamicTexture(nativeImage);

                            ResourceLocation loc = Minecraft.getInstance().getTextureManager()
                                    .register("thumb_" + UUID.randomUUID(), texture);

                            thumbnails.put(file, loc);
                            img.flush();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}