package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Data.ClientImageCache;
import org.akorpuzz.board.Data.ClientProfileCache;
import org.akorpuzz.board.Network.C2SRequestFeedPayload;
import org.akorpuzz.board.Network.C2SRequestImagePayload;
import org.akorpuzz.board.Network.C2SRequestPlayerListPayload;
import org.akorpuzz.board.Network.C2SRequestProfilePayload;
import org.akorpuzz.board.Network.S2CPlayerListPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HubScreen extends BoardScreen {

    private final String playerName;

    private static final ResourceLocation DEFAULT_AVATAR =
            ResourceLocation.fromNamespaceAndPath("board", "textures/gui/default_avatar.png");

    // Layout idéntico al resto de pantallas
    private static final int SIDEBAR_W  = 85;
    private static final int HEADER_H   = 60;
    private static final int PROFILE_SZ = 36;

    // Área central
    private int areaX, areaY, areaW, areaH;

    // Botones de texto en el sidebar
    private int feedX, feedY, feedW, feedH;
    private int newEntryX, newEntryY, newEntryW, newEntryH;
    private int myProfileX, myProfileY, myProfileW, myProfileH;

    // Lista de jugadores y control de imágenes solicitadas
    private List<S2CPlayerListPayload.PlayerEntry> playerList = new ArrayList<>();
    private final Set<String> requestedAvatars = new HashSet<>();

    // Scroll
    private int scrollOffset = 0;

    // Tarjetas
    private static final int CARD_H    = 44;
    private static final int CARD_GAP  = 8;
    private static final int CARD_COLS = 3;

    public HubScreen(String playerName) {
        super(Component.literal("Board — Hub"));
        this.playerName = playerName;
    }

    /** Llamado por ClientHandlers cuando llega la lista del servidor */
    public void setPlayerList(List<S2CPlayerListPayload.PlayerEntry> list) {
        this.playerList = list;
        // Solicitar imágenes que aún no estén en caché
        fetchMissingAvatars();
    }

    /** Solicita al servidor las imágenes de perfil que faltan en caché */
    private void fetchMissingAvatars() {
        for (S2CPlayerListPayload.PlayerEntry p : playerList) {
            String imgId = p.imageId();
            if (imgId == null || imgId.equals("none")) continue;
            if (requestedAvatars.contains(imgId)) continue;
            if (ClientImageCache.getTexture(imgId) != null) continue; // ya está cacheada
            try {
                PacketDistributor.sendToServer(new C2SRequestImagePayload(UUID.fromString(imgId)));
                requestedAvatars.add(imgId);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected void init() {
        super.init();

        areaX = SIDEBAR_W;
        areaY = HEADER_H + 5;
        areaW = this.width - areaX - 10;
        areaH = this.height - areaY - 10;

        feedX = 5;      feedY = 38;
        feedW = this.font.width("Feed");
        feedH = this.font.lineHeight;

        newEntryX = 5;  newEntryY = 52;
        newEntryW = this.font.width("New Entry");
        newEntryH = this.font.lineHeight;

        myProfileX = 5; myProfileY = 66;
        myProfileW = this.font.width("My Profile");
        myProfileH = this.font.lineHeight;

        // Solicitar la lista de jugadores al abrir el hub
        PacketDistributor.sendToServer(new C2SRequestPlayerListPayload());
    }

    // =========================================================================
    // RENDER
    // =========================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(g, mouseX, mouseY, partialTicks);

        renderHeader(g);
        renderSidebar(g, mouseX, mouseY);
        renderPlayersArea(g, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, this.width, HEADER_H, 0xEE050505);
        drawDoubleHorizontalLine(g, 0, this.width, HEADER_H);
        g.drawString(this.font, "§lTHE BOARD", 10, 25, 0xFFFFFF, true);

        // Mini perfil del jugador local
        String imgId = ClientProfileCache.getLocalProfileImageId();
        ResourceLocation photo = imgId.equals("none")
                ? DEFAULT_AVATAR : ClientImageCache.getTexture(imgId);
        if (photo == null) photo = DEFAULT_AVATAR;

        int pX = areaX + 5, pY = 12;
        g.fill(pX, pY, pX + PROFILE_SZ, pY + PROFILE_SZ, 0x33FFFFFF);
        g.blit(photo, pX, pY, 0, 0, PROFILE_SZ, PROFILE_SZ, PROFILE_SZ, PROFILE_SZ);
        g.renderOutline(pX, pY, PROFILE_SZ, PROFILE_SZ, 0x88FFFFFF);
        g.drawString(this.font, "§6" + playerName,
                pX + PROFILE_SZ + 10, pY + PROFILE_SZ / 2 - 4, 0xFFFFFF, true);
    }

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        drawDoubleVerticalLine(g, SIDEBAR_W - 2, HEADER_H, this.height);

        if (drawTextButton(g, "Feed", feedX, feedY, mouseX, mouseY))
            renderCustomTooltip(g, "Load entries", mouseX, mouseY);
        if (drawTextButton(g, "New Entry", newEntryX, newEntryY, mouseX, mouseY))
            renderCustomTooltip(g, "Make a new entry", mouseX, mouseY);
        if (drawTextButton(g, "My profile", myProfileX, myProfileY, mouseX, mouseY))
            renderCustomTooltip(g, "Load my profile", mouseX, mouseY);

        g.hLine(4, SIDEBAR_W - 8, myProfileY + myProfileH + 6, 0x33FFFFFF);
        g.drawString(this.font, "§8PLAYERS", 5, myProfileY + myProfileH + 10, 0x555555, false);

        int online = playerList.size();
        String label = online + (online == 1 ? " player" : " players");
        g.drawString(this.font, "§7" + label, 5, this.height - 20, 0x555555, false);
    }

    private void renderPlayersArea(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, 0xAA101010);
        drawRectBorder(g, areaX, areaY, areaW, areaH, 0x22FFFFFF);

        g.drawString(this.font, "§7PLAYERS IN SERVER",
                areaX + 10, areaY + 6, 0x888888, false);
        g.hLine(areaX + 5, areaX + areaW - 5, areaY + 18, 0x22FFFFFF);

        int contentY = areaY + 24;
        int contentH = areaH - 24;

        if (playerList.isEmpty()) {
            g.drawCenteredString(this.font, "§8Loading players...",
                    areaX + areaW / 2, contentY + contentH / 2, 0x444444);
            return;
        }

        g.enableScissor(areaX, contentY, areaX + areaW, contentY + contentH);

        int padding    = 10;
        int totalGapsW = CARD_GAP * (CARD_COLS - 1);
        int cardW      = (areaW - padding * 2 - totalGapsW) / CARD_COLS;

        for (int i = 0; i < playerList.size(); i++) {
            int col   = i % CARD_COLS;
            int row   = i / CARD_COLS;
            int cardX = areaX + padding + col * (cardW + CARD_GAP);
            int cardY = contentY + padding + row * (CARD_H + CARD_GAP) - scrollOffset;
            if (cardY + CARD_H < contentY || cardY > contentY + contentH) continue;
            renderPlayerCard(g, playerList.get(i), cardX, cardY, cardW, mouseX, mouseY);
        }

        g.disableScissor();

        // Barra de scroll
        int totalRows     = (int) Math.ceil((double) playerList.size() / CARD_COLS);
        int totalContentH = totalRows * (CARD_H + CARD_GAP) + padding * 2;
        int maxScroll     = Math.max(0, totalContentH - contentH);
        if (maxScroll > 0) {
            int barH = Math.max(20, (int) ((float) contentH / totalContentH * contentH));
            int barY = contentY + (int) ((float) scrollOffset / maxScroll * (contentH - barH));
            g.fill(areaX + areaW - 3, barY, areaX + areaW - 1, barY + barH, 0x55FFAA00);
        }
    }

    private void renderPlayerCard(GuiGraphics g,
            S2CPlayerListPayload.PlayerEntry player,
            int x, int y, int w, int mouseX, int mouseY) {

        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + CARD_H;
        boolean isMe    = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getUUID().equals(player.uuid());

        // Fondo y borde
        g.fill(x, y, x + w, y + CARD_H, hovered ? 0x44FFAA00 : (isMe ? 0x2200AAFF : 0x22FFFFFF));
        drawRectBorder(g, x, y, w, CARD_H, hovered ? 0xAAFFAA00 : (isMe ? 0x556688FF : 0x33FFFFFF));

        // Avatar — usar imagen real del perfil si está en caché, si no default
        int avatarSize = CARD_H - 8;
        int avatarX    = x + 4;
        int avatarY    = y + 4;

        ResourceLocation avatar = DEFAULT_AVATAR;
        String imgId = player.imageId();
        if (imgId != null && !imgId.equals("none")) {
            ResourceLocation cached = ClientImageCache.getTexture(imgId);
            if (cached != null) avatar = cached;
            // Si no está cacheada aún se muestra el default — llegará en cuanto se descargue
        }

        g.fill(avatarX - 1, avatarY - 1, avatarX + avatarSize + 1, avatarY + avatarSize + 1,
                isMe ? 0x55FFAA00 : 0x33FFFFFF);
        g.blit(avatar, avatarX, avatarY, 0, 0, avatarSize, avatarSize, avatarSize, avatarSize);

        // Nombre
        int nameX = avatarX + avatarSize + 6;
        int nameY = y + CARD_H / 2 - this.font.lineHeight;
        g.drawString(this.font,
                isMe ? "§6" + player.name() + " §8(you)" : "§f" + player.name(),
                nameX, nameY, 0xFFFFFF, false);

        if (hovered) {
            g.drawString(this.font, "§8See profile →", nameX, nameY + this.font.lineHeight + 2, 0x888888, false);
        }
    }

    // =========================================================================
    // INPUT
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOver(mouseX, mouseY, feedX, feedY, feedW, feedH)) {
            PacketDistributor.sendToServer(new C2SRequestFeedPayload());
            return true;
        }
        if (isOver(mouseX, mouseY, newEntryX, newEntryY, newEntryW, newEntryH)) {
            Minecraft.getInstance().setScreen(new NewEntry());
            return true;
        }
        if (isOver(mouseX, mouseY, myProfileX, myProfileY, myProfileW, myProfileH)) {
            PacketDistributor.sendToServer(
                    new C2SRequestProfilePayload(Minecraft.getInstance().player.getUUID()));
            return true;
        }

        // Clic en tarjeta de jugador
        if (!playerList.isEmpty()) {
            int contentY   = areaY + 24;
            int padding    = 10;
            int totalGapsW = CARD_GAP * (CARD_COLS - 1);
            int cardW      = (areaW - padding * 2 - totalGapsW) / CARD_COLS;

            for (int i = 0; i < playerList.size(); i++) {
                int col   = i % CARD_COLS;
                int row   = i / CARD_COLS;
                int cardX = areaX + padding + col * (cardW + CARD_GAP);
                int cardY = contentY + padding + row * (CARD_H + CARD_GAP) - scrollOffset;
                if (mouseX >= cardX && mouseX <= cardX + cardW
                        && mouseY >= cardY && mouseY <= cardY + CARD_H) {
                    PacketDistributor.sendToServer(
                            new C2SRequestProfilePayload(playerList.get(i).uuid()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= areaX && mouseX <= areaX + areaW
                && mouseY >= areaY && mouseY <= areaY + areaH) {
            int totalRows     = (int) Math.ceil((double) playerList.size() / CARD_COLS);
            int totalContentH = totalRows * (CARD_H + CARD_GAP) + 20;
            int maxScroll     = Math.max(0, totalContentH - (areaH - 24));
            scrollOffset -= (int) (scrollY * 16);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
