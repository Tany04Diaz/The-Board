package org.akorpuzz.board.Screens;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Network.C2SImageChunkPayload;
import org.akorpuzz.board.Network.C2SInputPayload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NewEntry extends Screen {
    private MultiLineEditBox textField;
    private Button SaveBtn;
    private Path selectedImagePath;
    private List<File> localImages = new ArrayList<>();
    private int currentImageIndex = -1;
    private String statusMessage = "Selecciona una imagend de la carpeta";
    private List<File> selectedImages = new ArrayList<>();
    int totalWidth = 200;
    int gap = 4;
    int folderWidth = 50;
    int selectWidth = totalWidth - folderWidth - gap;

    public NewEntry() {
        super(Component.literal("Nueva Entrada"));
    }
    @Override
    protected void init() {
        super.init();
        refreshLocalImages();
        // --- LAYOUT ---
        int centerX = this.width / 2;
        int startY = 40;
        //1. cuadro de texto
        this.textField = new MultiLineEditBox(
                this.font,
                centerX - 100, startY,
                200, 80,
                Component.literal("Escribe aquí..."),
                Component.empty()
        );
        this.addRenderableWidget(this.textField);
        this.textField.setValueListener(s -> {
            if (s.length() > 2000) {
                this.textField.setValue(s.substring(0, 2000));
            }
        });
        int totalWidth = 200;
        int gap = 5;
        int selectWidth = 150;
        int folderWidth = 45;
// Fila 1: Seleccionar Imágenes y Carpeta
        this.addRenderableWidget(Button.builder(Component.literal("Seleccionar Imágenes"), (btn) -> {
            Minecraft.getInstance().setScreen(new ImageSelectionScreen(this, localImages, selectedImages));
        }).bounds(centerX - 100, startY + 85, selectWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Carpeta"), (btn) -> {
            Util.getPlatform().openFile(new File(Minecraft.getInstance().gameDirectory, "board_images"));
        }).bounds(centerX - 100 + selectWidth + gap, startY + 85, folderWidth, 20).build());

// Fila 2: Publicar Entrada y Cancelar (X)
        int xWidth = 20; // Botón X cuadrado
        int publishWidth = totalWidth - xWidth - gap;

        this.addRenderableWidget(Button.builder(Component.literal("Publicar Entrada"), btn -> {
            processAndSend();
        }).bounds(centerX - 100, startY + 115, publishWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("X"), (btn) -> {
            Minecraft.getInstance().setScreen(new HubScreen(Minecraft.getInstance().getUser().getName()));
        }).bounds(centerX - 100 + publishWidth + gap, startY + 115, xWidth, 20).build());
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(this.font, "Nueva Publicación", this.width / 2, 15, 0xFFAA00);

        // Mensaje de estado
        guiGraphics.drawCenteredString(this.font, this.statusMessage, this.width / 2, 185, 0xFFFFFF);

        if (localImages.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§8(Añade .png a .minecraft/board_images/)", this.width / 2, 200, 0x777777);
        }
        if (!selectedImages.isEmpty()) {
            int listY = 200;
            for (File f : selectedImages) {
                String name = f.getName();
                // Si el nombre es muy largo, lo cortamos
                if (name.length() > 30) name = name.substring(0, 27) + "...";

                guiGraphics.drawCenteredString(this.font, "§7• " + name, this.width / 2, listY, 0xAAAAAA);
                listY += 10;
            }
        }
    }
    private void sendImageInChunks(Path path, UUID imageId){
        try{
            byte[] allBytes = java.nio.file.Files.readAllBytes(path);
            int chunkSize = 16384; //16kbXchunk
            int totalChunks = (int) Math.ceil((double) allBytes.length/chunkSize);

            for(int i = 0; i< totalChunks; i++){
                int start = i * chunkSize;
                int end = Math.min(allBytes.length, start + chunkSize);
                byte[] chunk = java.util.Arrays.copyOfRange(allBytes,start,end);

                PacketDistributor.sendToServer(new C2SImageChunkPayload(imageId,i,totalChunks,chunk));
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    private void processAndSend() {
        String text = this.textField.getValue().trim();
        if (text.isEmpty()) return;

        long day = minecraft.level.getDayTime() / 24000;
        String playerName = minecraft.player.getName().getString();
        UUID entryId = UUID.randomUUID();

        // 1. Crear una lista para almacenar los nuevos IDs de imagen
        List<String> imageIds = new ArrayList<>();

        // 2. Procesar cada archivo seleccionado
        for (File file : selectedImages) {
            UUID imageUuid = UUID.randomUUID();
            imageIds.add(imageUuid.toString());
            // Enviar los bytes de esta imagen específica
            sendImageInChunks(file.toPath(), imageUuid);
        }

        // 3. Formatear el campo imageId: "uuid1,uuid2,uuid3" o "none"
        String imageIdStr = imageIds.isEmpty() ? "none" : String.join(",", imageIds);

        // 4. Enviar el Payload de la entrada con la cadena de IDs
        PacketDistributor.sendToServer(new C2SInputPayload(
                day,
                text,
                playerName,
                entryId,
                imageIdStr
        ));

        Minecraft.getInstance().setScreen(new HubScreen(playerName));
    }    private void refreshLocalImages() {
        File uploadDir = new File(Minecraft.getInstance().gameDirectory, "board_images");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        File[] files = uploadDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));

        this.localImages = (files != null) ? List.of(files) : new ArrayList<>();
    }
    public void setSelectedImages(List<File> images) {
        this.selectedImages = images;
        if (images.isEmpty()) {
            this.statusMessage = "Selecciona una imagen de la carpeta";
        } else {
            this.statusMessage = "§aImágenes seleccionadas: §f" + images.size();
        }
    }

}
