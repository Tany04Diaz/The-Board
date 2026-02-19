package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Data.*;
import org.akorpuzz.board.Network.C2SRequestImagePayload;
import org.akorpuzz.board.Network.S2CUpdateProfilePayload;

import java.util.*;

public class UserProfileScreen extends BoardScreen {

    private final S2CUpdateProfilePayload profileData;
    private final List<FeedEntry> userEntries;
    private final Set<String> requestedImages = new HashSet<>();

    // ¿Es el perfil del jugador local? Controla si se muestra el botón de editar
    private final boolean isOwnProfile;

    private static final ResourceLocation DEFAULT_AVATAR =
            ResourceLocation.fromNamespaceAndPath("board", "textures/gui/default_avatar.png");

    // Layout — igual que FeedScreen
    private static final int SIDEBAR_W = 85;   // ancho del panel lateral
    private static final int HEADER_H = 60;    // alto del header superior
    private static final int AVATAR_SIZE = 52; // tamaño del avatar en el panel central

    // Área de posts con scroll
    private int areaX, areaY, areaWidth, areaHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Botones de texto en el sidebar
    private int volverX, volverY, volverW, volverH;
    private int editarX, editarY, editarW, editarH;

    // Constantes de post
    private static final int POST_AREA_WIDTH_MARGIN = 30;
    private static final int IMG_MAX_HEIGHT = 100;

    public UserProfileScreen(S2CUpdateProfilePayload data, List<FeedEntry> globalFeed) {
        super(Component.literal("Perfil de " + data.name()));
        this.profileData = data;
        this.userEntries = ClientFeedCache.getEntriesByPlayer(data.name());
        this.isOwnProfile = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getUUID().equals(data.uuid());
    }

    @Override
    protected void init() {
        super.init();

        // Área de contenido — igual que FeedScreen
        areaX = SIDEBAR_W;
        areaY = HEADER_H + 5;
        areaWidth = this.width - areaX - 10;
        areaHeight = this.height - areaY - 10;

        // Posiciones de botones de texto en el sidebar
        volverX = 5; volverY = 38;
        volverW = this.font.width("< Volver");
        volverH = this.font.lineHeight;

        editarX = 5; editarY = 52;
        editarW = this.font.width("Editar perfil");
        editarH = this.font.lineHeight;

        // Botón "Editar perfil" solo si es el perfil propio — lo añadimos como widget real
        // para que sea clicable y consistente con el resto de la UI
        if (isOwnProfile) {
            this.addRenderableWidget(
                Button.builder(Component.literal("✏ Editar perfil"), (btn) -> {
                    // Abrir EditProfileScreen pasando los datos actuales del perfil
                    Minecraft.getInstance().setScreen(
                            new EditProfileScreen(profileData,
                                    Minecraft.getInstance().player.getName().getString()));
                })
                .bounds(5, 70, 74, 16)
                .build()
            );
        }

        // Solicitar imágenes una sola vez en init
        preFetchImages();
    }

    private void preFetchImages() {
        // Avatar del perfil
        requestImageIfNeeded(profileData.imageId());
        // Imágenes de los posts
        for (FeedEntry entry : userEntries) {
            if (entry.imageId() != null && !entry.imageId().equals("none")) {
                for (String id : entry.imageId().split(",")) {
                    requestImageIfNeeded(id.trim());
                }
            }
        }
    }

    private void requestImageIfNeeded(String imgId) {
        if (imgId == null || imgId.isEmpty() || imgId.equals("none")) return;
        if (requestedImages.contains(imgId)) return;
        if (ClientImageCache.getTexture(imgId) != null) return;
        try {
            PacketDistributor.sendToServer(new C2SRequestImagePayload(UUID.fromString(imgId)));
            requestedImages.add(imgId);
        } catch (IllegalArgumentException ignored) {}
    }

    // =========================================================================
    // RENDER
    // =========================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // Fondo semitransparente
        g.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(g, mouseX, mouseY, partialTicks);

        renderHeader(g);
        renderSidebar(g, mouseX, mouseY);
        renderProfilePanel(g);
        renderPostsArea(g);
    }

    // ── Header superior ──────────────────────────────────────────────────────

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, this.width, HEADER_H, 0xEE050505);
        drawDoubleHorizontalLine(g, 0, this.width, HEADER_H);

        // Título
        g.drawString(this.font, "§lTHE BOARD", 10, 25, 0xFFFFFF, true);

        // Nombre del perfil en el centro del header
        g.drawCenteredString(this.font,
                "§6" + profileData.name().toUpperCase(),
                this.width / 2, 22, 0xFFFFFF);

        // Badge "propio" si es el perfil del jugador local
        if (isOwnProfile) {
            String badge = "[ tu perfil ]";
            int bw = this.font.width(badge);
            g.drawString(this.font, "§7" + badge,
                    this.width / 2 - bw / 2, 34, 0x888888, false);
        }
    }

    // ── Sidebar izquierdo ────────────────────────────────────────────────────

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        drawDoubleVerticalLine(g, SIDEBAR_W - 2, HEADER_H, this.height);

        // Botón "< Volver" como texto clicable
        if (drawTextButton(g, "< Volver", volverX, volverY, mouseX, mouseY)) {
            renderCustomTooltip(g, "Volver al hub", mouseX, mouseY);
        }

        // "Editar perfil" como texto solo si NO hay widget (perfiles ajenos)
        // Para el propio ya existe el widget Button añadido en init()
        if (!isOwnProfile) {
            g.drawString(this.font, "§8Editar perfil", editarX, editarY, 0x444444, false);
        }

        // Contador de publicaciones
        String count = userEntries.size() + " post" + (userEntries.size() != 1 ? "s" : "");
        g.drawString(this.font, "§7" + count, 5, this.height - 20, 0x555555, false);
    }

    // ── Panel central del perfil (avatar + nombre + bio) ────────────────────

    private void renderProfilePanel(GuiGraphics g) {
        int panelX = areaX;
        int panelY = areaY;
        int panelW = areaWidth;
        int panelH = 110; // alto fijo del bloque de perfil

        // Fondo del panel de perfil
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xAA101010);
        drawRectBorder(g, panelX, panelY, panelW, panelH, 0x33FFFFFF);

        int centerX = panelX + panelW / 2;

        // Avatar
        String imgId = profileData.imageId();
        ResourceLocation avatar = (imgId == null || imgId.equals("none"))
                ? DEFAULT_AVATAR
                : ClientImageCache.getTexture(imgId);
        if (avatar == null) avatar = DEFAULT_AVATAR;

        int avatarX = centerX - AVATAR_SIZE / 2;
        int avatarY = panelY + 8;

        // Marco del avatar
        g.fill(avatarX - 2, avatarY - 2, avatarX + AVATAR_SIZE + 2, avatarY + AVATAR_SIZE + 2, 0x55FFAA00);
        g.blit(avatar, avatarX, avatarY, 0, 0, AVATAR_SIZE, AVATAR_SIZE, AVATAR_SIZE, AVATAR_SIZE);

        // Nombre
        g.drawCenteredString(this.font,
                "§6§l" + profileData.name().toUpperCase(),
                centerX, avatarY + AVATAR_SIZE + 6, 0xFFFFFF);

        // Bio
        String bio = profileData.bio() != null ? profileData.bio() : "";
        List<FormattedCharSequence> bioLines = this.font.split(
                Component.literal(bio), panelW - 30);
        int bioY = avatarY + AVATAR_SIZE + 18;
        for (FormattedCharSequence line : bioLines) {
            g.drawCenteredString(this.font, line, centerX, bioY, 0xAAAAAA);
            bioY += this.font.lineHeight + 1;
        }

        // Línea separadora antes de los posts
        int sepY = panelY + panelH;
        g.fill(panelX + 10, sepY, panelX + panelW - 10, sepY + 1, 0x33FFFFFF);
        g.drawString(this.font,
                "§7PUBLICACIONES  §8(" + userEntries.size() + ")",
                panelX + 12, sepY + 4, 0x888888, false);
    }

    // ── Área de posts con scroll ─────────────────────────────────────────────

    private void renderPostsArea(GuiGraphics g) {
        // El área de posts empieza debajo del panel de perfil
        int postsAreaY = areaY + 110 + 14; // panel + separador + label
        int postsAreaH = this.height - postsAreaY - 10;

        // Fondo
        g.fill(areaX, postsAreaY, areaX + areaWidth, postsAreaY + postsAreaH, 0xAA101010);
        drawRectBorder(g, areaX, postsAreaY, areaWidth, postsAreaH, 0x22FFFFFF);

        // Clip
        g.enableScissor(areaX, postsAreaY, areaX + areaWidth, postsAreaY + postsAreaH);

        if (userEntries.isEmpty()) {
            g.drawCenteredString(this.font,
                    "§8Sin publicaciones todavía",
                    areaX + areaWidth / 2, postsAreaY + 20, 0x444444);
            maxScroll = 0;
        } else {
            int currentY = postsAreaY + 8 - scrollOffset;

            for (FeedEntry entry : userEntries) {
                int entryH = getEntryHeight(entry);
                if (currentY + entryH > postsAreaY && currentY < postsAreaY + postsAreaH) {
                    renderPost(g, entry, currentY);
                }
                currentY += entryH;
            }

            // Calcular maxScroll
            int totalH = (postsAreaY + 8 - scrollOffset) + scrollOffset;
            // Recalcular sumando alturas reales
            int sum = 0;
            for (FeedEntry e : userEntries) sum += getEntryHeight(e);
            maxScroll = Math.max(0, sum - postsAreaH + 16);
        }

        g.disableScissor();

        // Barra de scroll
        if (maxScroll > 0) {
            int barH = Math.max(20, (int) ((float) postsAreaH / (postsAreaH + maxScroll) * postsAreaH));
            int barY = postsAreaY + (int) ((float) scrollOffset / maxScroll * (postsAreaH - barH));
            g.fill(areaX + areaWidth - 3, barY, areaX + areaWidth - 1, barY + barH, 0x66FFAA00);
        }
    }

    private void renderPost(GuiGraphics g, FeedEntry entry, int y) {
        int postX = areaX + 10;
        int postW = areaWidth - 20;

        // Día
        long day = (entry.day() / 24000) + 1;
        g.drawString(this.font, "§6Día " + day, postX, y, 0xFFAA00, false);
        int curY = y + this.font.lineHeight + 3;

        // Texto
        List<FormattedCharSequence> lines = this.font.split(
                Component.literal(entry.text()), postW - 10);
        for (FormattedCharSequence line : lines) {
            g.drawString(this.font, line, postX + 5, curY, 0xDDDDDD, false);
            curY += this.font.lineHeight;
        }
        curY += 6;

        // Imágenes
        if (entry.imageId() != null && !entry.imageId().equals("none")) {
            List<String> ids = new ArrayList<>();
            for (String id : entry.imageId().split(",")) {
                if (!id.trim().isEmpty()) ids.add(id.trim());
            }
            if (!ids.isEmpty()) {
                int gap = 6;
                int containerW = (postW - 10 - gap * (ids.size() - 1)) / ids.size();
                int imgX = postX + 5;
                for (String imgId : ids) {
                    var tex = ClientImageCache.getTexture(imgId);
                    var size = ClientImageCache.getSize(imgId);
                    g.fill(imgX, curY, imgX + containerW, curY + IMG_MAX_HEIGHT, 0x22FFFFFF);
                    if (tex != null && size != null) {
                        float scale = Math.min(
                                (float) containerW / size.width(),
                                (float) IMG_MAX_HEIGHT / size.height());
                        int rW = (int) (size.width() * scale);
                        int rH = (int) (size.height() * scale);
                        g.blit(tex,
                                imgX + (containerW - rW) / 2,
                                curY + (IMG_MAX_HEIGHT - rH) / 2,
                                0, 0, rW, rH, rW, rH);
                    } else {
                        g.drawCenteredString(this.font, "...",
                                imgX + containerW / 2,
                                curY + IMG_MAX_HEIGHT / 2 - 4, 0x33FFFFFF);
                    }
                    imgX += containerW + gap;
                }
                curY += IMG_MAX_HEIGHT + 8;
            }
        }

        // Divisor
        curY += 4;
        g.hLine(postX + 5, postX + postW - 5, curY, 0x22FFFFFF);
    }

    private int getEntryHeight(FeedEntry entry) {
        int postW = areaWidth - 20;
        List<FormattedCharSequence> lines = this.font.split(
                Component.literal(entry.text()), postW - 10);
        int textH = lines.size() * this.font.lineHeight + 6;
        int imgH = (entry.imageId() == null || entry.imageId().equals("none")) ? 0 : (IMG_MAX_HEIGHT + 8);
        return this.font.lineHeight + 3 + textH + imgH + 4 + 8; // día + texto + img + divisor + margen
    }

    // =========================================================================
    // INPUT
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Botón "< Volver"
        if (isMouseOver(mouseX, mouseY, volverX, volverY, volverW, volverH)) {
            Minecraft.getInstance().setScreen(
                    new HubScreen(Minecraft.getInstance().player.getName().getString()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) (scrollY * 16);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        return true;
    }

    private boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
