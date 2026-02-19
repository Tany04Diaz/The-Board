package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Data.ClientImageCache;
import org.akorpuzz.board.Data.ClientProfileCache;
import org.akorpuzz.board.Data.FeedEntry;
import org.akorpuzz.board.Data.ImageSize;
import org.akorpuzz.board.Network.C2SRequestEntryPayload;
import org.akorpuzz.board.Network.C2SRequestImagePayload;

import java.util.*;

public class FeedScreen extends BoardScreen {
    private static final ResourceLocation DEFAULT_AVATAR =
            ResourceLocation.fromNamespaceAndPath("board", "textures/gui/default_avatar.png");

    // Layout
    private static final int SIDEBAR_W  = 85;
    private static final int HEADER_H   = 60;
    private static final int PROFILE_SZ = 36;

    // Botones de texto del sidebar
    private int hubX, hubY, hubW, hubH;
    private int feedX, feedY, feedW, feedH;
    private int newEntryX, newEntryY, newEntryW, newEntryH;

    // Área del feed con scroll
    private int areaX, areaY, areaWidth, areaHeight;
    private int scrollOffset = 0;

    private final List<FeedEntry> entries;
    private final String playerName;
    private String profileImageId;
    private final Set<String> requestedImages = new HashSet<>();

    public FeedScreen(String playerName, List<FeedEntry> entries, String profileImageId) {
        super(Component.literal("Feed"));
        this.playerName = playerName;
        this.entries = entries;
        this.profileImageId = profileImageId;
    }

    @Override
    protected void init() {
        super.init();

        hubX = 5; hubY = 38;
        hubW = this.font.width("< Hub");
        hubH = this.font.lineHeight;

        feedX = 5; feedY = 52;
        feedW = this.font.width("Update");
        feedH = this.font.lineHeight;

        newEntryX = 5; newEntryY = 66;
        newEntryW = this.font.width("New Entry");
        newEntryH = this.font.lineHeight;

        areaX = SIDEBAR_W;
        areaY = HEADER_H + 5;
        areaWidth  = this.width - areaX - 10;
        areaHeight = this.height - areaY - 10;

        preFetchImages();
    }

    private void preFetchImages() {
        requestImageIfNeeded(profileImageId);
        for (FeedEntry entry : entries) {
            if (entry.imageId() != null && !entry.imageId().equals("none")) {
                for (String id : entry.imageId().split(","))
                    requestImageIfNeeded(id.trim());
            }
        }
    }

    // =========================================================================
    // RENDER
    // =========================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        g.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(g, mouseX, mouseY, partialTicks);

        renderHeader(g);
        drawDoubleVerticalLine(g, SIDEBAR_W - 2, HEADER_H, this.height);
        renderSidebar(g, mouseX, mouseY);
        renderFeedArea(g);
        drawRectBorder(g, areaX, areaY, areaWidth, areaHeight, 0x22FFFFFF);
    }

    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, this.width, HEADER_H, 0xEE050505);
        drawDoubleHorizontalLine(g, 0, this.width, HEADER_H);
        g.drawString(this.font, "§lTHE BOARD", 10, 25, 0xFFFFFF, true);

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
        if (drawTextButton(g, "< Hub", hubX, hubY, mouseX, mouseY))
            renderCustomTooltip(g, "Go back to Hub", mouseX, mouseY);

        g.hLine(4, SIDEBAR_W - 8, hubY + hubH + 4, 0x22FFFFFF);

        if (drawTextButton(g, "Update", feedX, feedY, mouseX, mouseY))
            renderCustomTooltip(g, "Reload Feed", mouseX, mouseY);

        if (drawTextButton(g, "New Entry", newEntryX, newEntryY, mouseX, mouseY))
            renderCustomTooltip(g, "Make a new entry", mouseX, mouseY);
    }

    private void renderFeedArea(GuiGraphics g) {
        g.fill(areaX, areaY, areaX + areaWidth, areaY + areaHeight, 0xAA101010);
        g.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);

        if (entries.isEmpty()) {
            g.drawCenteredString(this.font, "§8No entries",
                    areaX + areaWidth / 2, areaY + areaHeight / 2, 0x444444);
        } else {
            int currentY = areaY - scrollOffset;
            for (FeedEntry entry : entries) {
                renderEntry(g, entry, currentY);
                currentY += calculateEntryHeight(entry);
            }
        }

        g.disableScissor();
    }

    // ── Entrada individual ───────────────────────────────────────────────────

    private void renderEntry(GuiGraphics g, FeedEntry entry, int y) {
        List<String> validIds = new ArrayList<>();
        if (entry.imageId() != null && !entry.imageId().equals("none")) {
            for (String id : entry.imageId().split(","))
                if (!id.trim().isEmpty()) validIds.add(id.trim());
        }

        g.drawString(this.font,
                "§6" + entry.playerName() + " §7- Day " + entry.day(),
                areaX + 10, y, 0xFFFFFF);
        int curY = y + this.font.lineHeight + 5;

        List<FormattedCharSequence> lines = this.font.split(
                Component.literal(entry.text()), areaWidth - 30);
        for (FormattedCharSequence line : lines) {
            g.drawString(this.font, line, areaX + 15, curY, 0xDDDDDD, false);
            curY += this.font.lineHeight;
        }
        curY += 8;

        if (!validIds.isEmpty()) {
            int gap = 6, maxH = 100;
            int cW = (areaWidth - 40 - gap * (validIds.size() - 1)) / validIds.size();
            int imgX = areaX + 15;
            for (String imgId : validIds) {
                var tex  = ClientImageCache.getTexture(imgId);
                ImageSize size = ClientImageCache.getSize(imgId);
                g.fill(imgX, curY, imgX + cW, curY + maxH, 0x22FFFFFF);
                if (tex != null && size != null) {
                    float scale = Math.min((float) cW / size.width(), (float) maxH / size.height());
                    int rW = (int)(size.width() * scale), rH = (int)(size.height() * scale);
                    g.blit(tex, imgX + (cW - rW)/2, curY + (maxH - rH)/2,
                            0, 0, rW, rH, rW, rH);
                } else {
                    g.drawCenteredString(this.font, "...",
                            imgX + cW/2, curY + maxH/2 - 4, 0x44FFFFFF);
                }
                imgX += cW + gap;
            }
            curY += maxH + 10;
        }

        curY += 5;
        g.hLine(areaX + 20, areaX + areaWidth - 20, curY, 0x22FFFFFF);
    }

    private int calculateEntryHeight(FeedEntry entry) {
        List<FormattedCharSequence> lines = this.font.split(
                Component.literal(entry.text()), areaWidth - 30);
        int textH   = lines.size() * this.font.lineHeight;
        int imagesH = (entry.imageId() != null && !entry.imageId().equals("none")) ? 110 : 0;
        return this.font.lineHeight + textH + imagesH + 43;
    }

    // =========================================================================
    // INPUT
    // =========================================================================

    private void requestImageIfNeeded(String imgId) {
        if (imgId == null || imgId.isEmpty() || imgId.equals("none")) return;
        if (requestedImages.contains(imgId) || ClientImageCache.getTexture(imgId) != null) return;
        try {
            PacketDistributor.sendToServer(new C2SRequestImagePayload(UUID.fromString(imgId)));
            requestedImages.add(imgId);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= areaX && mouseX <= areaX + areaWidth
                && mouseY >= areaY && mouseY <= areaY + areaHeight) {
            scrollOffset -= (int)(scrollY * 16);
            if (scrollOffset < 0) scrollOffset = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOver(mouseX, mouseY, hubX, hubY, hubW, hubH)) {
            Minecraft.getInstance().setScreen(new HubScreen(playerName));
            return true;
        }
        if (isOver(mouseX, mouseY, feedX, feedY, feedW, feedH)) {
            PacketDistributor.sendToServer(new C2SRequestEntryPayload(
                    Minecraft.getInstance().player.getUUID()));
            return true;
        }
        if (isOver(mouseX, mouseY, newEntryX, newEntryY, newEntryW, newEntryH)) {
            Minecraft.getInstance().setScreen(new NewEntry());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
