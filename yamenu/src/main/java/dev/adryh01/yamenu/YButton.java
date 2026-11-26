package dev.adryh01.yamenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

final class YButton extends GuiButton {

    private float hover;

    YButton(int id, int x, int y, int w, int h, String text) {
        super(id, x, y, w, h, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my) {
        if (!visible) return;
        boolean over = enabled && mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        hover += ((over ? 1.0F : 0.0F) - hover) * 0.35F;
        int fill = UI.lerp(0xFFFFFFFF, 0xFFEDEDED, hover);
        int border = UI.lerp(UI.BORDER, UI.ACCENT, hover);
        UI.box(xPosition, yPosition, xPosition + width, yPosition + height, 5, border, fill);
        int col = enabled ? UI.DARK : UI.SOFT;
        mc.fontRendererObj.drawString(displayString, xPosition + (width - mc.fontRendererObj.getStringWidth(displayString)) / 2,
            yPosition + (height - 8) / 2, col);
    }
}
