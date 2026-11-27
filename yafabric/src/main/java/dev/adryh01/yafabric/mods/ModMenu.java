package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/** The YA MODS menu on Fabric: categories, module list with switches and a settings page. */
public class ModMenu extends Screen {

    private static final String[] CATS = {"Info", "PvP", "Utility", "Visual"};
    private static int cat;
    private static Module selected;
    private int scrollL, scrollR;
    private Setting dragging;
    private int dragX, dragW;

    private static final int PW = 400, PH = 250;

    private int px() {
        return (width - PW) / 2;
    }

    private int py() {
        return (height - PH) / 2;
    }

    private List<Module> list() {
        List<Module> l = new ArrayList<Module>();
        for (Module m : Mods.ALL) {
            if (m.category.equals(CATS[cat])) l.add(m);
        }
        return l;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    @Override
    public void render(int mx, int my, float delta) {
        TextRenderer fr = MinecraftClient.getInstance().textRenderer;
        DrawableHelper.fill(0, 0, width, height, 0x70000000);
        int x = px(), y = py();
        UI.roundBox(x, y, x + PW, y + PH, 6, UI.BORDER, UI.BG, 1);
        fr.draw("YA MODS", x + 12, y + 10, UI.DARK);
        int tx = x + 90;
        for (int i = 0; i < CATS.length; i++) {
            int w = fr.getStringWidth(CATS[i]) + 14;
            boolean on = i == cat;
            UI.roundRect(tx, y + 6, tx + w, y + 22, 4, on ? UI.DARK : (in(mx, my, tx, y + 6, w, 16) ? UI.CHIP : UI.HEADER));
            fr.draw(CATS[i], tx + 7, y + 10, on ? 0xFFFFFFFF : UI.MID);
            tx += w + 4;
        }
        // module list (left)
        int lx = x + 10, ly = y + 30, lw = 170, lh = PH - 40;
        UI.roundBox(lx, ly, lx + lw, ly + lh, 4, UI.BORDER, UI.CARD, 1);
        List<Module> mods = list();
        int total = mods.size() * 20;
        scrollL = Math.max(0, Math.min(scrollL, Math.max(0, total - (lh - 4))));
        int sy = ly + 2 - scrollL;
        for (Module m : mods) {
            if (sy + 20 > ly && sy < ly + lh - 2) {
                boolean sel = m == selected, hv = in(mx, my, lx + 2, sy, lw - 4, 18) && my > ly && my < ly + lh;
                if (sel || hv) UI.roundRect(lx + 2, sy, lx + lw - 2, sy + 18, 3, sel ? UI.HEADER : UI.CARD_HOVER);
                String nm = fr.trimToWidth(m.name, lw - 48);
                fr.draw(nm, lx + 8, sy + 5, UI.DARK);
                m.knob += ((m.enabled ? 1.0F : 0.0F) - m.knob) * 0.4F;
                UI.sw(lx + lw - 30, sy + 3, m.knob);
            }
            sy += 20;
        }
        // settings (right)
        int rx = x + 190, rw = PW - 200;
        UI.roundBox(rx, ly, rx + rw, ly + lh, 4, UI.BORDER, UI.CARD, 1);
        if (selected == null) {
            fr.draw("Pick a module on the left.", rx + 10, ly + 10, UI.SOFT);
        } else {
            fr.draw(selected.name, rx + 10, ly + 8, UI.DARK);
            for (String line : fr.wrapLines(selected.desc, rw - 20)) {
                // only the first two lines
                if (line == null) break;
            }
            List<String> d = fr.wrapLines(selected.desc, rw - 20);
            int dy = ly + 22;
            for (int i = 0; i < Math.min(2, d.size()); i++, dy += 10) fr.draw(d.get(i), rx + 10, dy, UI.SOFT);
            int top = dy + 4;
            int rowsH = 0;
            for (Setting s : selected.settings) if (!s.hidden) rowsH += 20;
            int view = ly + lh - top - 2;
            scrollR = Math.max(0, Math.min(scrollR, Math.max(0, rowsH - view)));
            int ry = top - scrollR;
            for (Setting s : selected.settings) {
                if (s.hidden) continue;
                if (ry + 18 > top && ry < ly + lh - 2) drawSetting(fr, s, rx + 8, ry, rw - 16, mx, my);
                ry += 20;
            }
        }
    }

    private void drawSetting(TextRenderer fr, Setting s, int x, int y, int w, int mx, int my) {
        boolean hv = in(mx, my, x, y, w, 18);
        UI.roundRect(x, y, x + w, y + 18, 3, hv ? UI.HEADER : UI.CARD_HOVER);
        fr.draw(fr.trimToWidth(s.label, w - 70), x + 6, y + 5, UI.DARK);
        if (s.type == Setting.BOOL) {
            UI.sw(x + w - 28, y + 3, s.b ? 1.0F : 0.0F);
        } else if (s.type == Setting.SLIDER) {
            int bx = x + w - 84, bw = 50;
            DrawableHelper.fill(bx, y + 8, bx + bw, y + 10, 0xFFCFCFCF);
            int kx = bx + (int) (bw * s.fraction());
            DrawableHelper.fill(bx, y + 8, kx, y + 10, UI.DARK);
            UI.roundRect(kx - 2, y + 5, kx + 2, y + 13, 2, UI.DARK);
            String v = s.display();
            fr.draw(v, x + w - 4 - fr.getStringWidth(v), y + 5, UI.MID);
        } else {
            String v = s.display();
            fr.draw(v, x + w - 6 - fr.getStringWidth(v), y + 5, UI.MID);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (button != 0) return;
        int x = px(), y = py();
        {
            TextRenderer fr = MinecraftClient.getInstance().textRenderer;
            int tx = x + 90;
            for (int i = 0; i < CATS.length; i++) {
                int w = fr.getStringWidth(CATS[i]) + 14;
                if (in(mx, my, tx, y + 6, w, 16)) {
                    cat = i;
                    scrollL = 0;
                    selected = null;
                    return;
                }
                tx += w + 4;
            }
        }
        int lx = x + 10, ly = y + 30, lw = 170, lh = PH - 40;
        if (in(mx, my, lx, ly, lw, lh)) {
            int sy = ly + 2 - scrollL;
            for (Module m : list()) {
                if (in(mx, my, lx + 2, sy, lw - 4, 18)) {
                    if (mx >= lx + lw - 34) {
                        m.setEnabled(!m.enabled);
                        Mods.save();
                    } else {
                        selected = m;
                        scrollR = 0;
                    }
                    return;
                }
                sy += 20;
            }
            return;
        }
        int rx = x + 190, rw = PW - 200;
        if (selected != null && in(mx, my, rx, ly, rw, lh)) {
            TextRenderer fr = MinecraftClient.getInstance().textRenderer;
            int dy = ly + 22 + 10 * Math.min(2, fr.wrapLines(selected.desc, rw - 20).size());
            int ry = dy + 4 - scrollR;
            for (Setting s : selected.settings) {
                if (s.hidden) continue;
                if (in(mx, my, rx + 8, ry, rw - 16, 18)) {
                    if (s.type == Setting.SLIDER) {
                        dragging = s;
                        dragX = rx + 8 + rw - 16 - 84;
                        dragW = 50;
                        s.setFraction((mx - dragX) / (float) dragW);
                    } else if (s.type == Setting.ACTION) {
                        if (s.run != null) s.run.run();
                    } else {
                        s.cycle(1);
                    }
                    return;
                }
                ry += 20;
            }
        }
    }

    @Override
    protected void mouseDragged(int mx, int my, int button, long time) {
        if (dragging != null) dragging.setFraction((mx - dragX) / (float) dragW);
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (dragging != null) Mods.save();
        dragging = null;
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / MinecraftClient.getInstance().width;
        int step = d > 0 ? -20 : 20;
        if (mx < px() + 185) scrollL = Math.max(0, scrollL + step);
        else scrollR = Math.max(0, scrollR + step);
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == 1 || key == 54) {
            Mods.save();
            MinecraftClient.getInstance().setScreen(null);
        }
    }
}
