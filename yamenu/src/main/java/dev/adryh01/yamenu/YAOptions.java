package dev.adryh01.yamenu;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/** Basic options screen in the YA style; the sub screens stay vanilla. */
final class YAOptions extends YAScreen {

    private boolean drag;
    private boolean blurOn;

    YAOptions(GuiScreen parent) {
        super(parent, "Options");
    }

    private int sx() {
        return width / 2 - 100;
    }

    private int sy() {
        return 62;
    }

    @Override
    public void initGui() {
        if (mc.theWorld != null && OpenGlHelper.shadersSupported && !blurOn) {
            try {
                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
                blurOn = true;
            } catch (Exception ignored) {
            }
        }
        buttonList.clear();
        int x = width / 2 - 100, y = 100;
        buttonList.add(new YButton(1, x, y, 98, 22, "Video settings"));
        buttonList.add(new YButton(2, x + 102, y, 98, 22, "Controls"));
        buttonList.add(new YButton(3, x, y + 27, 98, 22, "Sound"));
        buttonList.add(new YButton(4, x + 102, y + 27, 98, 22, "Language"));
        buttonList.add(new YButton(5, x, y + 54, 98, 22, "Skin"));
        buttonList.add(new YButton(6, x + 102, y + 54, 98, 22, "Resource packs"));
        buttonList.add(new YButton(7, x, y + 81, 200, 22, "Chat settings"));
        buttonList.add(new YButton(0, x, y + 117, 200, 22, "Done"));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        mc.gameSettings.saveOptions();
        switch (b.id) {
            case 1:
                mc.displayGuiScreen(new GuiVideoSettings(this, mc.gameSettings));
                break;
            case 2:
                mc.displayGuiScreen(new GuiControls(this, mc.gameSettings));
                break;
            case 3:
                mc.displayGuiScreen(new GuiScreenOptionsSounds(this, mc.gameSettings));
                break;
            case 4:
                mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager()));
                break;
            case 5:
                mc.displayGuiScreen(new GuiCustomizeSkin(this));
                break;
            case 6:
                mc.displayGuiScreen(new GuiScreenResourcePacks(this));
                break;
            case 7:
                mc.displayGuiScreen(new ScreenChatOptions(this, mc.gameSettings));
                break;
            case 0:
                mc.displayGuiScreen(parent);
                break;
            default:
        }
    }

    @Override
    public void onGuiClosed() {
        if (blurOn) {
            mc.entityRenderer.stopUseShader();
            blurOn = false;
        }
    }

    private void setFov(int mx) {
        float f = Math.max(0.0F, Math.min(1.0F, (mx - sx()) / 200.0F));
        mc.gameSettings.fovSetting = Math.round(30.0F + f * 80.0F);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button == 0 && mx >= sx() && mx <= sx() + 200 && my >= sy() && my <= sy() + 22) {
            drag = true;
            setFov(mx);
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (drag) setFov(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        if (drag) {
            drag = false;
            mc.gameSettings.saveOptions();
        }
        super.mouseReleased(mx, my, state);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        if (mc.theWorld == null) {
            drawGradientRect(0, 0, width, height, 0xFFF8F8F8, 0xFFD6D9DE);
        } else {
            // the world behind is blurred by the shader; a light veil keeps the text readable
            Gui.drawRect(0, 0, width, height, 0x50F3F3F3);
        }
        Gui.drawRect(0, 0, width, 34, 0xB0E9E9E9);
        Gui.drawRect(0, 34, width, 35, UI.BORDER);
        UI.logo(10, 4, 26);
        fontRendererObj.drawString("Options", 44, 13, UI.DARK);
        int x = sx(), y = sy();
        UI.box(x, y, x + 200, y + 22, 5, UI.BORDER, 0xFFFFFFFF);
        float f = (mc.gameSettings.fovSetting - 30.0F) / 80.0F;
        Gui.drawRect(x + 8, y + 15, x + 192, y + 17, 0xFFD0D0D0);
        Gui.drawRect(x + 8, y + 15, x + 8 + (int) (184 * f), y + 17, UI.ACCENT);
        int kx = x + 8 + (int) (184 * f);
        UI.roundRect(kx - 3, y + 11, kx + 3, y + 21, 2, UI.ACCENT);
        String t = "FOV: " + (int) mc.gameSettings.fovSetting;
        fontRendererObj.drawString(t, x + 8, y + 4, UI.DARK);
        super.drawScreen(mx, my, pt);
    }
}
