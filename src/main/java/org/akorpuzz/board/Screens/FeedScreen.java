package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Data.ClientImageCache;
import org.akorpuzz.board.Data.FeedEntry;
import org.akorpuzz.board.Network.C2SRequestEntryPayload;

import java.util.*;

public class FeedScreen extends BoardScreen {
    private int feedX, feedY, feedW, feedH;
    private int demoX, demoY, demoW, demoH;
    private int areaX, areaY, areaWidth, areaHeight, scrollOffset = 0;
    private List<FeedEntry> entries;
    private final String Feed = "Feed";
    private final String playerName;
    private final Set<String> requestedImages = new HashSet<>();

    public FeedScreen(String playerName, List<FeedEntry> entries) {
        super(Component.literal("Feed"));
        this.playerName = playerName;
        this.entries = entries;
    }

    @Override
    protected void init() {
        super.init();
        feedX = 5; feedY = 38;
        feedW = this.font.width("Feed");
        feedH = this.font.lineHeight;

        demoX = 5; demoY = 48;
        demoW = this.font.width("Nueva entrada");
        demoH = this.font.lineHeight;

        areaX = 85;
        areaY = 50;
        areaWidth = this.width - areaX - 10;
        areaHeight = this.height - areaY - 10;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 1. Fondo general y clase padre (botones de la izquierda)
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 2. Decoración estética (Líneas blancas dobles)
        drawDoubleHorizontalLine(guiGraphics, 0, this.width, 30);
        drawDoubleVerticalLine(guiGraphics, 80, 0, this.height);

        // 3. CONFIGURACIÓN DEL ÁREA DE FEED (El contenedor)
        // areaX suele ser 90 (dejando espacio para la barra lateral de 80)
        guiGraphics.fill(areaX, areaY, areaX + areaWidth, areaY + areaHeight, 0xAA101010);

        // Habilitar Scissor para que nada se salga del cuadro al hacer scroll
        guiGraphics.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);

        int y = areaY - scrollOffset;

        for (FeedEntry entry : entries) {
            // Filtrar IDs válidos
            List<String> validIds = new ArrayList<>();
            if (entry.imageId() != null && !entry.imageId().equals("none")) {
                for (String id : entry.imageId().split(",")) {
                    if (!id.trim().isEmpty()) validIds.add(id.trim());
                }
            }

            // --- CÁLCULO DE ALTURA DINÁMICA ---
            List<FormattedCharSequence> lineasDivididas = this.font.split(Component.literal(entry.text()), areaWidth - 30);
            int textoHeight = lineasDivididas.size() * this.font.lineHeight;

            int gap = 6;
            int maxContainerH = 100; // Altura del "cuadro" de imagen
            // Dividimos el ancho disponible entre la cantidad de imágenes
            int containerW = validIds.isEmpty() ? 0 : (areaWidth - 40 - (gap * (validIds.size() - 1))) / validIds.size();

            // Altura total de esta publicación específica
            int entryTotalHeight = this.font.lineHeight + 8 + textoHeight + (validIds.isEmpty() ? 0 : maxContainerH + 10) + 20;

            // Solo renderizamos si es visible en pantalla (Optimización)
            if (y + entryTotalHeight >= areaY && y <= areaY + areaHeight) {

                // A. Dibujar Encabezado (Nombre y Día)
                guiGraphics.drawString(this.font, "§6" + entry.playerName() + " §7- Día " + entry.day(), areaX + 10, y, 0xFFFFFF);
                y += this.font.lineHeight + 5;

                // B. Dibujar Cuerpo del Texto
                for (FormattedCharSequence linea : lineasDivididas) {
                    guiGraphics.drawString(this.font, linea, areaX + 15, y, 0xDDDDDD, false);
                    y += this.font.lineHeight;
                }
                y += 8;

                // C. RENDERIZADO DE IMÁGENES (Contenedores del dibujo)
                if (!validIds.isEmpty()) {
                    int currentX = areaX + 15;

                    for (String imgId : validIds) {
                        ResourceLocation texture = ClientImageCache.getTexture(imgId);
                        // Importante: Usamos la info de tamaño que ya tienes en el cache
                        org.akorpuzz.board.Data.ImageSize size = ClientImageCache.getSize(imgId);

                        // Dibujar el fondo del contenedor de la imagen (rectángulo gris oscuro)
                        guiGraphics.fill(currentX, y, currentX + containerW, y + maxContainerH, 0x22FFFFFF);

                        if (texture != null && size != null) {
                            // LÓGICA DE ESCALADO PROPORCIONAL
                            float scale = Math.min((float) containerW / size.width(), (float) maxContainerH / size.height());
                            int renderW = (int) (size.width() * scale);
                            int renderH = (int) (size.height() * scale);

                            // Centrar dentro del contenedor
                            int offsetX = (containerW - renderW) / 2;
                            int offsetY = (maxContainerH - renderH) / 2;

                            guiGraphics.blit(texture, currentX + offsetX, y + offsetY, 0, 0, renderW, renderH, renderW, renderH);
                        } else {
                            // Si no existe, solicitarla y mostrar carga
                            if (!requestedImages.contains(imgId)) {
                                try {
                                    PacketDistributor.sendToServer(new org.akorpuzz.board.Network.C2SRequestImagePayload(UUID.fromString(imgId)));
                                    requestedImages.add(imgId);
                                } catch (Exception ignored) {}
                            }
                            guiGraphics.drawCenteredString(this.font, "...", currentX + (containerW / 2), y + (maxContainerH / 2) - 4, 0x44FFFFFF);
                        }
                        currentX += containerW + gap;
                    }
                    y += maxContainerH + 10;
                }

                // D. Línea divisoria entre posts
                y += 5;
                guiGraphics.hLine(areaX + 20, areaX + areaWidth - 20, y, 0x22FFFFFF);
                y += 10;

            } else {
                // Si no es visible, simplemente saltamos la altura para el siguiente post
                y += entryTotalHeight;
            }
        }

        guiGraphics.disableScissor();

        // 4. UI Fija (Título y tooltips que no deben moverse con el scroll)
        renderFixedUI(guiGraphics, mouseX, mouseY);
    }    private void renderFixedUI(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawRectBorder(guiGraphics, areaX, areaY, areaWidth, areaHeight, 0xFFFFFFFF);

        guiGraphics.drawCenteredString(this.font, "The Board", this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Hola " + playerName, 5, 20, 0xFFFFFF, true);

        if (drawTextButton(guiGraphics, "Feed", feedX, feedY, mouseX, mouseY)) {
            renderCustomTooltip(guiGraphics, "Recent Updates", mouseX, mouseY);
        }

        if (drawTextButton(guiGraphics, "Nueva entrada", demoX, demoY, mouseX, mouseY)) {
            renderCustomTooltip(guiGraphics, "Crea una nueva entrada", mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= areaX && mouseX <= areaX + areaWidth && mouseY >= areaY && mouseY <= areaY + areaHeight) {
            scrollOffset -= (int) (scrollY * 16); // Scroll un poco más rápido
            if (scrollOffset < 0) scrollOffset = 0;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, feedX, feedY, feedW, feedH)) {
            PacketDistributor.sendToServer(new C2SRequestEntryPayload(Minecraft.getInstance().player.getUUID()));
            return true;
        }

        if (isMouseOver(mouseX, mouseY, demoX, demoY, demoW, demoH)) {
            Minecraft.getInstance().setScreen(new NewEntry());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}