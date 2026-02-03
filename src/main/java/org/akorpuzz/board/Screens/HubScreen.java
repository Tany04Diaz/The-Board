package org.akorpuzz.board.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Network.C2SRequestEntryPayload;

public class HubScreen extends BoardScreen {
    private final String playerName;
    private int feedX, feedY, feedW, feedH;
    private int demoX, demoY, demoW, demoH;

    public HubScreen(String playerName) {
        super(Component.literal("Board"));
        this.playerName = playerName;
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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Usando los métodos de la clase padre
        drawDoubleHorizontalLine(guiGraphics, 0, this.width, 30);
        drawDoubleVerticalLine(guiGraphics, 80, 0, this.height);

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