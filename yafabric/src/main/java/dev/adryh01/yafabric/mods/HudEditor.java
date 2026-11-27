package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

import java.util.List;

/** Drag the HUD modules where you want them. ESC goes back to the menu. */
public class HudEditor extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private Module drag;
    private int offX, offY;

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    @Override
    public void removed() {
        Mods.save();
    }

    @Override
    public void render(int mx, int my, float pt) {
        DrawableHelper.fill(0, 0, width, height, 0x50000000);
        Hud.renderAll(width, height, true);

        for (Module m : Mods.ALL) {
            if (!m.hud || !m.enabled) continue;
            boolean over = m == drag || (drag == null && inside(m, mx, my) && top(mx, my) == m);
            int c = over ? 0xFFFFFFFF : 0x90FFFFFF;
            int x1 = m.lastX - 2, y1 = m.lastY - 2, x2 = m.lastX + m.lastW + 2, y2 = m.lastY + m.lastH + 2;
            DrawableHelper.fill(x1, y1, x2, y1 + 1, c);
            DrawableHelper.fill(x1, y2 - 1, x2, y2, c);
            DrawableHelper.fill(x1, y1, x1 + 1, y2, c);
            DrawableHelper.fill(x2 - 1, y1, x2, y2, c);
            if (over) {
                TextRenderer fr = mc.textRenderer;
                int w = fr.getStringWidth(m.name) + 10;
                int ty = y1 - 14 < 2 ? y2 + 3 : y1 - 14;
                UI.roundRect(x1, ty, x1 + w, ty + 12, 4, UI.ACCENT);
                fr.draw(m.name, x1 + 5, ty + 2, 0xFFFFFFFF);
            }
        }

        TextRenderer fr = mc.textRenderer;
        String t1 = "HUD editor";
        String t2 = "Drag the modules to move them  -  ESC to go back";
        int w = Math.max(fr.getStringWidth(t2), fr.getStringWidth(t1) * 2) + 24;
        int x = (width - w) / 2;
        UI.roundRect(x, 8, x + w, 40, 6, UI.BG);
        fr.draw(t1, (width - fr.getStringWidth(t1)) / 2, 13, UI.DARK);
        fr.draw(t2, (width - fr.getStringWidth(t2)) / 2, 26, UI.MID);
    }

    private boolean inside(Module m, int mx, int my) {
        return mx >= m.lastX - 2 && mx <= m.lastX + m.lastW + 2 && my >= m.lastY - 2 && my <= m.lastY + m.lastH + 2;
    }

    /** Last (top-most) enabled HUD module under the mouse. */
    private Module top(int mx, int my) {
        List<Module> all = Mods.ALL;
        for (int i = all.size() - 1; i >= 0; i--) {
            Module m = all.get(i);
            if (m.hud && m.enabled && inside(m, mx, my)) return m;
        }
        return null;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        Module m = top(mx, my);
        if (m != null) {
            drag = m;
            offX = mx - m.lastX;
            offY = my - m.lastY;
        }
    }

    @Override
    protected void mouseDragged(int mx, int my, int button, long time) {
        if (drag == null) return;
        drag.moved = true;
        int freeX = width - drag.lastW;
        int freeY = height - drag.lastH;
        drag.xf = freeX <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, (mx - offX) / (float) freeX));
        drag.yf = freeY <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, (my - offY) / (float) freeY));
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        if (drag != null) {
            drag = null;
            Mods.save();
        }
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_RSHIFT && UI.shiftGuard()) return;
        if (key == Keyboard.KEY_ESCAPE) {
            mc.setScreen(new ModMenu());
            return;
        }
        if (key == Keyboard.KEY_RSHIFT) {
            mc.setScreen(null);
            return;
        }
        super.keyPressed(c, key);
    }
}
