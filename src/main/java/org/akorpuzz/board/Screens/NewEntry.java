package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Network.C2SInputPayload;
import org.akorpuzz.board.Network.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NewEntry extends Screen implements ImageSelectionScreen.ImageConsumer {
    private MultiLineEditBox textField;
    private List<File> localImages = new ArrayList<>();
    private String statusMessage = "select up to 3 images";
    private List<File> selectedImages = new ArrayList<>();
    private final String playerName;

    public NewEntry() {
        super(Component.literal("New entry"));
        this.playerName = Minecraft.getInstance().player.getName().getString();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // 1. Bajamos un poco el cuadro de texto para dar espacio al título
        this.textField = new MultiLineEditBox(this.font, centerX - 100, 45, 200, 80,
                Component.literal("text"), Component.literal("Write your history..."));
        this.addRenderableWidget(this.textField);

        // 2. Botón de Selección (Lo movemos más abajo del cuadro de texto)
        // Posición Y = 145 (deja espacio para las previsualizaciones si las añades luego)
        this.addRenderableWidget(Button.builder(Component.literal("Select Image"), (btn) -> {
            refreshLocalImages();
            Minecraft.getInstance().setScreen(new ImageSelectionScreen(this, this.localImages, this));
        }).bounds(centerX - 100, 150, 200, 20).build());

        // 3. Botones de Acción (Publicar y Cancelar)
        // Los bajamos a Y = 180 para que no choquen con el contador de imágenes
        this.addRenderableWidget(Button.builder(Component.literal("Post"), (btn) -> saveEntry())
                .bounds(centerX - 100, 185, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), (btn) ->
                        Minecraft.getInstance().setScreen(new HubScreen(playerName)))
                .bounds(centerX + 2, 185, 98, 20).build());
    }
    @Override
    public void onImagesSelected(List<File> files) {
        this.selectedImages = files; // Recibe la lista completa
        this.statusMessage = "Images Selected: " + selectedImages.size();
    }

    private void saveEntry() {
        UUID entryId = UUID.randomUUID();
        List<String> imageIds = new ArrayList<>();

        for (File file : selectedImages) {
            UUID imageUuid = UUID.randomUUID();
            imageIds.add(imageUuid.toString());
            NetworkUtils.sendImageInChunks(file.toPath(), imageUuid);
        }

        String imageIdStr = imageIds.isEmpty() ? "none" : String.join(",", imageIds);
        long worldTime = (Minecraft.getInstance().level.getGameTime()/24000);
        PacketDistributor.sendToServer(new C2SInputPayload(worldTime, textField.getValue(), playerName, entryId, imageIdStr));
        Minecraft.getInstance().setScreen(new HubScreen(playerName));
    }

    private void refreshLocalImages() {
        File uploadDir = new File(Minecraft.getInstance().gameDirectory, "board_images");
        if (!uploadDir.exists()) uploadDir.mkdirs();
        File[] files = uploadDir.listFiles((dir, name) -> {
            String l = name.toLowerCase();
            return l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg");
        });
        if (files != null) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            this.localImages = List.of(files);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Dibujamos un fondo oscuro tras el formulario para que resalte (opcional)
        int centerX = this.width / 2;
        graphics.fill(centerX - 110, 35, centerX + 110, 215, 0x88000000);

        // Título Superior
        graphics.drawCenteredString(this.font, "NEW ENTRY", centerX, 20, 0xFFFFFF);

        // MENSAJE DE ESTADO (Ajustado para que no se encime)
        // Lo colocamos justo encima del botón de seleccionar, en Y = 138
        graphics.drawCenteredString(this.font, this.statusMessage, centerX, 138, 0xAAAAAA);
    }}