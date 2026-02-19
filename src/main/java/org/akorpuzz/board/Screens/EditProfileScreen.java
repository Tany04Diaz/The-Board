package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Data.ClientImageCache;
import org.akorpuzz.board.Network.C2SSaveProfilePayload;
import org.akorpuzz.board.Network.S2CUpdateProfilePayload;
import org.akorpuzz.board.Network.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditProfileScreen extends BoardScreen implements ImageSelectionScreen.ImageConsumer {
    private final String playerName;
    private final String playerNameLocal;
    private final UUID playerUUID;
    private String currentImageId;
    private MultiLineEditBox bioEditor;
    private final String initialBio;
    private List<File> localImages = new ArrayList<>();
    private ResourceLocation localPreviewResource = null;

    private static final ResourceLocation DEFAULT_AVATAR =
            ResourceLocation.fromNamespaceAndPath("board", "textures/gui/default_avatar.png");

    public EditProfileScreen(S2CUpdateProfilePayload data, String playerName) {
        super(Component.literal("Editar Perfil"));
        this.playerUUID = data.uuid();
        this.currentImageId = data.imageId();
        this.initialBio = data.bio();
        this.playerName = playerName;
        this.playerNameLocal = Minecraft.getInstance().player.getName().getString();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        this.bioEditor = new MultiLineEditBox(
                this.font, centerX - 100, 125, 200, 60,
                Component.literal("Bio"), Component.literal("..."));
        this.bioEditor.setValue(this.initialBio != null ? this.initialBio : "");
        this.addRenderableWidget(this.bioEditor);

        this.addRenderableWidget(Button.builder(Component.literal("Cambiar Foto"), (btn) -> {
            refreshLocalImages();
            Minecraft.getInstance().setScreen(new ImageSelectionScreen(this, this.localImages, this));
        }).bounds(centerX - 40, 95, 80, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("X"), (btn) -> {
            this.currentImageId = "none";
            this.localPreviewResource = null;
        }).bounds(centerX + 28, 60, 20, 20)
          .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                  Component.literal("Resetear Foto"))).build());

        this.addRenderableWidget(Button.builder(Component.literal("Guardar"), (btn) -> {
            PacketDistributor.sendToServer(
                    new C2SSaveProfilePayload(this.bioEditor.getValue(), this.currentImageId));
            Minecraft.getInstance().setScreen(new HubScreen(playerNameLocal));
        }).bounds(centerX - 100, 195, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), (btn) ->
                        Minecraft.getInstance().setScreen(new HubScreen(playerNameLocal)))
                .bounds(centerX + 2, 195, 98, 20).build());
    }

    @Override
    public void onImagesSelected(List<File> files) {
        if (files.isEmpty()) return;
        File file = files.get(0); // El perfil solo usa la primera imagen

        UUID newImageId = UUID.randomUUID();
        this.currentImageId = newImageId.toString();

        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
            if (img != null) {
                DynamicTexture texture = new DynamicTexture(ClientImageCache.convertToNative(img));
                this.localPreviewResource = Minecraft.getInstance()
                        .getTextureManager().register("preview_" + newImageId, texture);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        NetworkUtils.sendImageInChunks(file.toPath(), newImageId);
        Minecraft.getInstance().setScreen(this);
    }

    private void refreshLocalImages() {
        File uploadDir = new File(Minecraft.getInstance().gameDirectory, "board_images");
        if (!uploadDir.exists()) uploadDir.mkdirs();
        File[] files = uploadDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        });
        this.localImages = (files != null) ? List.of(files) : new ArrayList<>();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        super.render(graphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        drawDoubleHorizontalLine(graphics, 0, this.width, 30);
        graphics.drawCenteredString(this.font, "PERFIL DE: " + playerName.toUpperCase(), centerX, 15, 0xFFFFFF);
        graphics.drawString(this.font, "Tu Biografía:", centerX - 100, 115, 0xAAAAAA);

        // Determinar qué foto mostrar: preview local → caché remota → default
        ResourceLocation photo;
        if (this.localPreviewResource != null) {
            photo = this.localPreviewResource;
        } else if (currentImageId == null || currentImageId.equals("none")) {
            photo = DEFAULT_AVATAR;
        } else {
            photo = ClientImageCache.getTexture(this.currentImageId);
            if (photo == null) photo = DEFAULT_AVATAR;
        }

        graphics.fill(centerX - 27, 43, centerX + 27, 93, 0xFFFFFFFF);
        graphics.blit(photo, centerX - 25, 45, 0, 0, 50, 50, 50, 50);
    }
}
