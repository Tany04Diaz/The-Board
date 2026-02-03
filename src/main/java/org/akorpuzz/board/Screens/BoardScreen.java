package org.akorpuzz.board.Screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BoardScreen extends Screen {

    protected BoardScreen(Component title) {
        super(title);
    }

    // --- LIBRERÍA DE DIBUJO REUTILIZABLE ---
    protected boolean drawTextButton(GuiGraphics graphics, String text, int x, int y, int mouseX, int mouseY) {
        int width = this.font.width(text);
        int height = this.font.lineHeight;
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int color = isHovered ? 0xFFFF00 : 0xFFFFFF; // Amarillo en hover, blanco normal
        graphics.drawString(this.font, text, x, y, color, true);

        return isHovered;
    }

    protected void drawDoubleHorizontalLine(GuiGraphics graphics, int xStart, int xEnd, int y) {
        graphics.hLine(xStart, xEnd, y, 0xFFFFFFFF);
        graphics.hLine(xStart, xEnd, y + 2, 0xFFFFFFFF);
    }

    protected void drawDoubleVerticalLine(GuiGraphics graphics, int x, int yStart, int yEnd) {
        graphics.vLine(x, yStart, yEnd, 0xFFFFFFFF);
        graphics.vLine(x + 2, yStart, yEnd, 0xFFFFFFFF);
    }

    protected void renderCustomTooltip(GuiGraphics graphics, String text, int mouseX, int mouseY) {
        graphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
    }

    protected void drawRectBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.hLine(x, x + w, y, color);
        graphics.hLine(x, x + w, y + h, color);
        graphics.vLine(x, y, y + h, color);
        graphics.vLine(x + w, y, y + h, color);
    }
}