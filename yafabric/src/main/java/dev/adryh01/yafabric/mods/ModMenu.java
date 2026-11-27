package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/** The YAMods menu (Right Shift): module cards with a switch and a settings page per module. */
public class ModMenu extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final String[] CATS = {"All", "Info", "PvP", "Utility", "Visual"};
    private static final int GAP = 8, CARD_H = 50, ROW_H = 22;

    private String category = "All";
    private String query = "";
    private boolean searching;
    private final List<Module> shown = new ArrayList<Module>();
    private Module open;          // module whose settings page is shown
    private Setting dragging;
    private Setting editing;      // text setting being typed into     // slider being dragged
    private float scroll, scrollTarget, gridScroll;
    private long openTime;

    private int px, py, pw, ph, gx, gy, gw, gh, cols, cardW;

    @Override
    public void init() {
        openTime = System.currentTimeMillis();
        rebuild();
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    @Override
    public void removed() {
        Mods.save();
    }

    private void rebuild() {
        shown.clear();
        for (Module m : Mods.ALL) {
            if (!(category.equals("All") || m.category.equals(category))) continue;
            if (query.length() > 0) {
                String q = query.toLowerCase();
                if (!m.name.toLowerCase().contains(q) && !m.desc.toLowerCase().contains(q)) continue;
            }
            shown.add(m);
        }
        scroll = 0.0F;
        scrollTarget = 0.0F;
    }

    private void layout() {
        pw = Math.min(460, width - 20);
        ph = Math.min(292, height - 20);
        float o = Math.min(1.0F, (System.currentTimeMillis() - openTime) / 230.0F);
        o = 1.0F - (1.0F - o) * (1.0F - o) * (1.0F - o);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1.0F - o) * 14.0F);
        gx = px + 14;
        gy = py + 64;
        gw = pw - 28 - 8;
        gh = ph - 64 - 40;
        cols = gw >= 300 ? 2 : 1;
        cardW = (gw - GAP * (cols - 1)) / cols;
    }

    private int maxScroll() {
        int rows = (shown.size() + cols - 1) / cols;
        return Math.max(0, rows * (CARD_H + GAP) - GAP - gh);
    }

    private int cardX(int i) {
        return gx + (i % cols) * (cardW + GAP);
    }

    private int cardY(int i) {
        return gy + (i / cols) * (CARD_H + GAP) - (int) scroll;
    }

    // ------------------------------------------------------------------ draw

    @Override
    public void render(int mx, int my, float pt) {
        layout();
        TextRenderer fr = mc.textRenderer;
        float o = Math.min(1.0F, (System.currentTimeMillis() - openTime) / 230.0F);

        DrawableHelper.fill(0, 0, width, height, ((int) (60 * o)) << 24);

        UI.roundRect(px + 2, py + 3, px + pw + 2, py + ph + 3, 8, 0x26000000);
        UI.roundRect(px, py, px + pw, py + ph, 8, UI.BG);
        UI.roundRect(px, py, px + pw, py + 36, 8, UI.HEADER);
        DrawableHelper.fill(px, py + 20, px + pw, py + 36, UI.HEADER);
        DrawableHelper.fill(px, py + 36, px + pw, py + 37, UI.LINE);

        GL11.glPushMatrix();
        GL11.glTranslatef(px + 14, py + 10, 0);
        GL11.glScalef(1.5F, 1.5F, 1.0F);
        fr.draw("YA MODS", 0, 0, UI.DARK);
        GL11.glPopMatrix();
        fr.draw("by " + "@Adryh01", px + 14 + (int) (fr.getStringWidth("YA MODS") * 1.5F) + 8, py + 15, UI.SOFT);

        int cbx = px + pw - 28, cby = py + 9;
        boolean overClose = mx >= cbx && mx < cbx + 18 && my >= cby && my < cby + 18;
        UI.roundRect(cbx, cby, cbx + 18, cby + 18, 4, overClose ? 0xFFD4D4D4 : UI.CHIP);
        fr.draw("x", cbx + 9 - fr.getStringWidth("x") / 2, cby + 5, UI.MID);

        if (open == null) drawGrid(mx, my, fr);
        else drawSettings(mx, my, fr);

        // footer
        int fy = py + ph - 26;
        DrawableHelper.fill(px, py + ph - 34, px + pw, py + ph - 33, UI.LINE);
        boolean overEdit = mx >= px + 14 && mx < px + 78 && my >= fy - 1 && my < fy + 15;
        UI.roundRect(px + 14, fy - 1, px + 78, fy + 15, 6, overEdit ? 0xFF4A4A4A : UI.ACCENT);
        fr.draw("Edit HUD", px + 46 - fr.getStringWidth("Edit HUD") / 2, fy + 3, 0xFFFFFFFF);
        String hint = "Right Shift to close";
        fr.draw(hint, px + pw - 14 - fr.getStringWidth(hint), fy + 3, UI.SOFT);
    }

    private void drawGrid(int mx, int my, TextRenderer fr) {
        int cx = px + 14;
        for (String c : CATS) {
            int cw = fr.getStringWidth(c) + 16;
            boolean sel = c.equals(category);
            boolean over = mx >= cx && mx < cx + cw && my >= py + 43 && my < py + 59;
            UI.roundRect(cx, py + 43, cx + cw, py + 59, 6, sel ? UI.ACCENT : (over ? 0xFFD8D8D8 : UI.CHIP));
            fr.draw(c, cx + 8, py + 47, sel ? 0xFFFFFFFF : UI.MID);
            cx += cw + 6;
        }

        int sbw = Math.min(120, Math.max(0, px + pw - 14 - cx - 4)), sbx = px + pw - 14 - sbw;
        if (sbw >= 70) {
            UI.roundBox(sbx, py + 43, sbx + sbw, py + 59, 6, searching ? UI.ACCENT : UI.BORDER, 0xFFFFFFFF, 1);
            String shownQ = query.length() == 0 && !searching ? "Search mods..." : query;
            if (searching && (System.currentTimeMillis() / 500) % 2 == 0) shownQ += "_";
            fr.draw(fr.trimToWidth(shownQ, sbw - 12, true), sbx + 6, py + 47, query.length() == 0 && !searching ? UI.SOFT : UI.DARK);
        }
        if (shown.isEmpty()) fr.draw("No mods found.", gx + 4, gy + 6, UI.SOFT);

        int max = maxScroll();
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.35F;
        if (Math.abs(scrollTarget - scroll) < 0.2F) scroll = scrollTarget;

        UI.scissor(mc, gx - 2, gy, gx + gw + 2, gy + gh);
        boolean inGrid = mx >= gx && mx < gx + gw && my >= gy && my < gy + gh;
        for (int i = 0; i < shown.size(); i++) {
            Module m = shown.get(i);
            int x = cardX(i), y = cardY(i);
            if (y + CARD_H < gy || y > gy + gh) continue;
            boolean over = inGrid && mx >= x && mx < x + cardW && my >= y && my < y + CARD_H;
            m.hover += ((over ? 1.0F : 0.0F) - m.hover) * 0.3F;
            m.knob += ((m.enabled ? 1.0F : 0.0F) - m.knob) * 0.35F;

            int border = m.enabled ? UI.ACCENT : UI.lerp(UI.BORDER, UI.SOFT, m.hover);
            UI.roundBox(x, y, x + cardW, y + CARD_H, 5, border, over ? UI.CARD_HOVER : UI.CARD, m.enabled ? 2 : 1);

            fr.draw(fr.trimToWidth(m.name, cardW - 50), x + 9, y + 8, UI.DARK);
            fr.draw(fr.trimToWidth(m.desc, cardW - 18), x + 9, y + 21, UI.MID);
            fr.draw(m.category, x + 9, y + CARD_H - 13, UI.SOFT);
            UI.sw(x + cardW - 32, y + 8, m.knob);

            int bx = x + cardW - 62, by = y + CARD_H - 18;
            boolean overBtn = over && mx >= bx && mx < bx + 54 && my >= by && my < by + 13;
            UI.roundRect(bx, by, bx + 54, by + 13, 5, overBtn ? 0xFFD4D4D4 : UI.CHIP);
            fr.draw("Settings", bx + 27 - fr.getStringWidth("Settings") / 2, by + 3, UI.MID);
        }
        UI.scissorOff();

        if (max > 0) {
            int tx = gx + gw + 4;
            DrawableHelper.fill(tx, gy, tx + 3, gy + gh, 0xFFE2E2E2);
            int rows = (shown.size() + cols - 1) / cols;
            int content = rows * (CARD_H + GAP) - GAP;
            int th = Math.max(18, gh * gh / content);
            int ty = gy + (int) ((gh - th) * (scroll / max));
            UI.roundRect(tx, ty, tx + 3, ty + th, 1, 0xFF9C9C9C);
        }
    }

    private void drawSettings(int mx, int my, TextRenderer fr) {
        boolean overBack = mx >= gx && mx < gx + 52 && my >= py + 43 && my < py + 59;
        UI.roundRect(gx, py + 43, gx + 52, py + 59, 6, overBack ? 0xFFD8D8D8 : UI.CHIP);
        fr.draw("< Back", gx + 26 - fr.getStringWidth("< Back") / 2, py + 47, UI.MID);

        GL11.glPushMatrix();
        GL11.glTranslatef(gx + 62, py + 44, 0);
        GL11.glScalef(1.25F, 1.25F, 1.0F);
        fr.draw(open.name, 0, 0, UI.DARK);
        GL11.glPopMatrix();
        String st = open.enabled ? "Enabled" : "Disabled";
        fr.draw(st, gx + gw - fr.getStringWidth(st) - 30, py + 47, open.enabled ? UI.DARK : UI.SOFT);
        open.knob += ((open.enabled ? 1.0F : 0.0F) - open.knob) * 0.35F;
        UI.sw(gx + gw - 24, py + 45, open.knob);

        scrollTarget = Math.max(0, Math.min(settingsMax(), scrollTarget));
        scroll += (scrollTarget - scroll) * 0.35F;
        if (Math.abs(scrollTarget - scroll) < 0.2F) scroll = scrollTarget;
        UI.scissor(mc, gx - 2, gy, gx + gw + 2, gy + gh);
        int y = gy + 4 - (int) scroll;
        for (Setting s : rows()) {
            boolean over = mx >= gx && mx < gx + gw && my >= Math.max(y, gy) && my < Math.min(y + ROW_H, gy + gh);
            UI.roundRect(gx, y, gx + gw, y + ROW_H - 2, 4, over ? UI.CARD_HOVER : UI.CARD);
            fr.draw(s.label, gx + 10, y + 7, UI.DARK);
            int right = gx + gw - 10;
            switch (s.type) {
                case Setting.BOOL:
                    UI.sw(right - 22, y + 4, s.b ? 1.0F : 0.0F);
                    break;
                case Setting.SLIDER: {
                    int tx = right - 150, tw = 100;
                    DrawableHelper.fill(tx, y + 9, tx + tw, y + 12, 0xFFD0D0D0);
                    int fill = (int) (tw * s.fraction());
                    DrawableHelper.fill(tx, y + 9, tx + fill, y + 12, UI.ACCENT);
                    UI.roundRect(tx + fill - 3, y + 5, tx + fill + 3, y + 15, 3, 0xFFFFFFFF);
                    UI.roundBox(tx + fill - 3, y + 5, tx + fill + 3, y + 15, 3, UI.ACCENT, 0xFFFFFFFF, 1);
                    String v = s.display();
                    fr.draw(v, right - fr.getStringWidth(v), y + 7, UI.MID);
                    break;
                }
                case Setting.ACTION: {
                    int bw = 106;
                    UI.roundRect(right - bw, y + 3, right, y + 17, 5, UI.ACCENT);
                    fr.draw("Open", right - bw / 2 - fr.getStringWidth("Open") / 2, y + 7, 0xFFFFFFFF);
                    break;
                }
                case Setting.TEXT: {
                    int bw = 106;
                    boolean ed = editing == s;
                    UI.roundBox(right - bw, y + 3, right, y + 17, 5, ed ? UI.ACCENT : UI.BORDER, 0xFFFFFFFF, 1);
                    String v = s.s.length() == 0 && !ed ? "click to type" : s.s;
                    if (ed && (System.currentTimeMillis() / 500) % 2 == 0) v += "_";
                    fr.draw(fr.trimToWidth(v, bw - 8, true), right - bw + 5, y + 7, s.s.length() == 0 && !ed ? UI.SOFT : UI.DARK);
                    break;
                }
                default: {
                    int bw = 106;
                    UI.roundRect(right - bw, y + 3, right, y + 17, 5, UI.CHIP);
                    String v = s.display();
                    int textX = right - bw / 2 - fr.getStringWidth(v) / 2;
                    if (s.type == Setting.COLOR) {
                        DrawableHelper.fill(right - bw + 6, y + 7, right - bw + 12, y + 13, 0xFF000000 | s.rgb());
                    }
                    fr.draw(v, textX + (s.type == Setting.COLOR ? 5 : 0), y + 7, UI.DARK);
                }
            }
            y += ROW_H;
        }
        UI.scissorOff();
    }

    // ----------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        layout();
        TextRenderer fr = mc.textRenderer;

        int cbx = px + pw - 28, cby = py + 9;
        if (button == 0 && mx >= cbx && mx < cbx + 18 && my >= cby && my < cby + 18) {
            click();
            mc.setScreen(null);
            return;
        }
        int fy = py + ph - 26;
        if (button == 0 && mx >= px + 14 && mx < px + 78 && my >= fy - 1 && my < fy + 15) {
            click();
            mc.setScreen(new HudEditor());
            return;
        }

        if (open != null) {
            clickSettings(mx, my, button);
            return;
        }
        if (button != 0) return;

        int sx0 = px + 14;
        for (String c : CATS) sx0 += fr.getStringWidth(c) + 22;
        int sbw = Math.min(120, Math.max(0, px + pw - 14 - sx0 - 4)), sbx = px + pw - 14 - sbw;
        searching = sbw >= 70 && mx >= sbx && mx < sbx + sbw && my >= py + 43 && my < py + 59;
        if (searching) return;

        int cx = px + 14;
        for (String c : CATS) {
            int cw = fr.getStringWidth(c) + 16;
            if (mx >= cx && mx < cx + cw && my >= py + 43 && my < py + 59) {
                if (!c.equals(category)) {
                    category = c;
                    rebuild();
                    click();
                }
                return;
            }
            cx += cw + 6;
        }

        if (mx >= gx && mx < gx + gw && my >= gy && my < gy + gh) {
            for (int i = 0; i < shown.size(); i++) {
                Module m = shown.get(i);
                int x = cardX(i), y = cardY(i);
                if (mx >= x && mx < x + cardW && my >= y && my < y + CARD_H) {
                    int bx = x + cardW - 62, by = y + CARD_H - 18;
                    if (mx >= bx && mx < bx + 54 && my >= by && my < by + 13) {
                        gridScroll = scrollTarget;
                        open = m;
                        scroll = 0.0F;
                        scrollTarget = 0.0F;
                    } else {
                        m.setEnabled(!m.enabled);
                        Mods.save();
                    }
                    click();
                    return;
                }
            }
        }
    }

    private void clickSettings(int mx, int my, int button) {
        editing = null;
        if (button == 0 && mx >= gx && mx < gx + 52 && my >= py + 43 && my < py + 59) {
            closeSettings();
            click();
            return;
        }
        if (button == 0 && mx >= gx + gw - 26 && mx < gx + gw && my >= py + 43 && my < py + 61) {
            open.setEnabled(!open.enabled);
            Mods.save();
            click();
            return;
        }
        if (my < gy || my >= gy + gh) return;
        int y = gy + 4 - (int) scroll;
        for (Setting s : rows()) {
            if (my >= y && my < y + ROW_H && mx >= gx && mx < gx + gw) {
                int right = gx + gw - 10;
                if (s.type == Setting.BOOL) {
                    s.cycle(1);
                    Mods.save();
                    click();
                } else if (s.type == Setting.SLIDER) {
                    if (button == 0) {
                        dragging = s;
                        sliderTo(s, mx);
                    }
                } else if (s.type == Setting.ACTION) {
                    if (mx >= right - 106 && s.run != null) {
                        click();
                        s.run.run();
                    }
                } else if (s.type == Setting.TEXT) {
                    if (mx >= right - 106) editing = s;
                } else if (mx >= right - 106) {
                    s.cycle(button == 1 ? -1 : 1);
                    Mods.save();
                    click();
                }
                return;
            }
            y += ROW_H;
        }
    }

    private void sliderTo(Setting s, int mx) {
        int tx = gx + gw - 10 - 150;
        s.setFraction((mx - tx) / 100.0F);
    }

    @Override
    protected void mouseDragged(int mx, int my, int button, long time) {
        if (dragging != null) sliderTo(dragging, mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        if (dragging != null) {
            dragging = null;
            Mods.save();
        }
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (editing != null) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_ESCAPE) {
                editing = null;
                Mods.save();
            } else if (key == Keyboard.KEY_BACK) {
                if (editing.s.length() > 0) editing.s = editing.s.substring(0, editing.s.length() - 1);
            } else if (net.minecraft.util.SharedConstants.isValidChar(c) && editing.s.length() < editing.maxLen) {
                editing.s += c;
            }
            return;
        }
        if (searching && open == null) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_ESCAPE) {
                searching = false;
                if (key == Keyboard.KEY_ESCAPE && query.length() > 0) {
                    query = "";
                    rebuild();
                }
            } else if (key == Keyboard.KEY_BACK) {
                if (query.length() > 0) {
                    query = query.substring(0, query.length() - 1);
                    rebuild();
                }
            } else if (key != Keyboard.KEY_RSHIFT && net.minecraft.util.SharedConstants.isValidChar(c) && query.length() < 20) {
                query += c;
                rebuild();
            }
            if (key != Keyboard.KEY_RSHIFT) return;
        }
        if (key == Keyboard.KEY_RSHIFT) {
            mc.setScreen(null);
            return;
        }
        if (key == Keyboard.KEY_ESCAPE && open != null) {
            closeSettings();
            return;
        }
        super.keyPressed(c, key);
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            scrollTarget -= Integer.signum(d) * (open == null ? CARD_H * 0.7F : ROW_H * 2.0F);
            scrollTarget = Math.max(0, Math.min(open == null ? maxScroll() : settingsMax(), scrollTarget));
        }
    }

    private void closeSettings() {
        open = null;
        scroll = gridScroll;
        scrollTarget = gridScroll;
    }

    private List<Setting> rows() {
        List<Setting> r = new ArrayList<Setting>();
        for (Setting s : open.settings) {
            if (!s.hidden) r.add(s);
        }
        return r;
    }

    private int settingsMax() {
        return Math.max(0, rows().size() * ROW_H + 4 - gh);
    }

    private void click() {
        mc.getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
    }
}
