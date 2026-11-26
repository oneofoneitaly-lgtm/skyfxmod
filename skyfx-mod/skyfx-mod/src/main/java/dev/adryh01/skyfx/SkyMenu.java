package dev.adryh01.skyfx;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The /skyfx menu: white/grey matte panel with category chips, live previews and two toggles. */
public class SkyMenu extends GuiScreen {

    private static final int BG = 0xFFF3F3F3;
    private static final int HEADER = 0xFFE9E9E9;
    private static final int CARD = 0xFFFFFFFF;
    private static final int CARD_HOVER = 0xFFFAFAFA;
    private static final int BORDER = 0xFFD6D6D6;
    private static final int LINE = 0xFFDDDDDD;
    private static final int DARK = 0xFF2E2E2E;
    private static final int MID = 0xFF6B6B6B;
    private static final int SOFT = 0xFF9A9A9A;
    private static final int ACCENT = 0xFF3A3A3A;
    private static final int CHIP = 0xFFE3E3E3;
    private static final int GAP = 8;

    private static final String[] CATS = {"All", "Anime", "Cartoon", "Realistic", "Fantasy", "Retro", "Void"};

    private String category = "All";
    private final List<Sky> shown = new ArrayList<Sky>();
    private float[] hover = new float[0];
    private float scroll, scrollTarget;
    private float knobAnim = -1.0F, knobFog = -1.0F;
    private long openTime;

    // layout
    private int px, py, pw, ph, gx, gy, gw, gh, cols, cardW, cardH, prevH;

    @Override
    public void initGui() {
        openTime = System.currentTimeMillis();
        rebuild();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void rebuild() {
        shown.clear();
        for (Sky s : Sky.ALL) {
            if (category.equals("All") || s.category.equals(category)) shown.add(s);
        }
        hover = new float[shown.size()];
        scroll = 0.0F;
        scrollTarget = 0.0F;
    }

    private void layout() {
        pw = Math.min(460, width - 20);
        ph = Math.min(292, height - 20);
        float open = Math.min(1.0F, (System.currentTimeMillis() - openTime) / 230.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open) * (1.0F - open);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1.0F - open) * 14.0F);
        gx = px + 14;
        gy = py + 64;
        gw = pw - 28 - 8;
        gh = ph - 64 - 40;
        cols = Math.max(2, (gw + GAP) / (92 + GAP));
        cardW = (gw - GAP * (cols - 1)) / cols;
        prevH = (int) (cardW * 0.5F);
        cardH = prevH + 8 + 20;
    }

    private int maxScroll() {
        int rows = (shown.size() + cols - 1) / cols;
        int content = rows * (cardH + GAP) - GAP;
        return Math.max(0, content - gh);
    }

    private int cardX(int i) {
        return gx + (i % cols) * (cardW + GAP);
    }

    private int cardY(int i) {
        return gy + (i / cols) * (cardH + GAP) - (int) scroll;
    }

    // --------------------------------------------------------------- drawing

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        FontRenderer fr = fontRendererObj;
        float open = Math.min(1.0F, (System.currentTimeMillis() - openTime) / 230.0F);

        Gui.drawRect(0, 0, width, height, ((int) (60 * open)) << 24);

        // panel
        Draw.roundRect(px + 2, py + 3, px + pw + 2, py + ph + 3, 8, 0x26000000);
        Draw.roundRect(px, py, px + pw, py + ph, 8, BG);
        Draw.roundRect(px, py, px + pw, py + 36, 8, HEADER);
        Gui.drawRect(px, py + 20, px + pw, py + 36, HEADER);
        Gui.drawRect(px, py + 36, px + pw, py + 37, LINE);

        // title
        GlStateManager.pushMatrix();
        GlStateManager.translate(px + 14, py + 10, 0);
        GlStateManager.scale(1.5F, 1.5F, 1.0F);
        fr.drawString("SkyFX", 0, 0, DARK);
        GlStateManager.popMatrix();
        fr.drawString("by " + SkyFX.AUTHOR, px + 14 + (int) (fr.getStringWidth("SkyFX") * 1.5F) + 8, py + 15, SOFT);

        // close button
        int cbx = px + pw - 28, cby = py + 9;
        boolean overClose = mx >= cbx && mx < cbx + 18 && my >= cby && my < cby + 18;
        Draw.roundRect(cbx, cby, cbx + 18, cby + 18, 4, overClose ? 0xFFD4D4D4 : CHIP);
        fr.drawString("x", cbx + 9 - fr.getStringWidth("x") / 2, cby + 5, MID);

        // category chips
        int cx = px + 14;
        for (String c : CATS) {
            int cw = fr.getStringWidth(c) + 16;
            boolean sel = c.equals(category);
            boolean over = mx >= cx && mx < cx + cw && my >= py + 43 && my < py + 59;
            Draw.roundRect(cx, py + 43, cx + cw, py + 59, 6, sel ? ACCENT : (over ? 0xFFD8D8D8 : CHIP));
            fr.drawString(c, cx + 8, py + 47, sel ? 0xFFFFFFFF : MID);
            cx += cw + 6;
        }

        // cards (scrollable area)
        int max = maxScroll();
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.35F;
        if (Math.abs(scrollTarget - scroll) < 0.2F) scroll = scrollTarget;

        int[] clip = {gx - 2, gy, gx + gw + 2, gy + gh};
        Draw.scissor(mc, clip[0], clip[1], clip[2], clip[3]);

        float t = Sky.timeSec();
        boolean inGrid = mx >= gx && mx < gx + gw && my >= gy && my < gy + gh;

        for (int i = 0; i < shown.size(); i++) {
            int x = cardX(i);
            int y = cardY(i);
            if (y + cardH < gy || y > gy + gh) continue;
            Sky s = shown.get(i);
            boolean sel = s == SkyFX.current;
            boolean over = inGrid && mx >= x && mx < x + cardW && my >= y && my < y + cardH;
            hover[i] += ((over ? 1.0F : 0.0F) - hover[i]) * 0.3F;

            int border = sel ? ACCENT : Sky.lerp(BORDER, SOFT, hover[i]) | 0xFF000000;
            Draw.roundBox(x, y, x + cardW, y + cardH, 5, border, over ? CARD_HOVER : CARD, sel ? 2 : 1);

            int pxa = x + 4, pya = y + 4, pxb = x + cardW - 4, pyb = y + 4 + prevH;
            Draw.preview(mc, s, pxa, pya, pxb, pyb, t, clip);
            Gui.drawRect(pxa, pyb, pxb, pyb + 1, LINE);

            String label = fr.trimStringToWidth(s.name, cardW - 14);
            fr.drawString(label, x + 6, pyb + 7, sel ? DARK : MID);

            if (sel) {
                int bx = pxb - 14, by = pya + 3;
                Draw.roundRect(bx, by, bx + 11, by + 11, 3, ACCENT);
                int[][] px2 = {{2, 5}, {3, 6}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}};
                for (int[] p : px2) {
                    Gui.drawRect(bx + p[0], by + p[1] - 1, bx + p[0] + 1, by + p[1] + 1, 0xFFFFFFFF);
                }
            }
        }
        Draw.scissorOff();

        // scrollbar
        if (max > 0) {
            int tx = gx + gw + 4;
            Gui.drawRect(tx, gy, tx + 3, gy + gh, 0xFFE2E2E2);
            int rows = (shown.size() + cols - 1) / cols;
            int content = rows * (cardH + GAP) - GAP;
            int thumbH = Math.max(18, gh * gh / content);
            int thumbY = gy + (int) ((gh - thumbH) * (scroll / max));
            Draw.roundRect(tx, thumbY, tx + 3, thumbY + thumbH, 1, 0xFF9C9C9C);
        }

        // footer
        int fy = py + ph - 26;
        Gui.drawRect(px, py + ph - 34, px + pw, py + ph - 33, LINE);
        knobAnim = ease(knobAnim, SkyFX.animations);
        knobFog = ease(knobFog, SkyFX.fogTint);
        int tx = px + 14;
        drawToggle(tx, fy, "Animations", knobAnim, mx, my);
        int tx2 = tx + 26 + fr.getStringWidth("Animations") + 18;
        drawToggle(tx2, fy, "Sky fog", knobFog, mx, my);

        String active = SkyFX.current == null ? "Vanilla" : SkyFX.current.name;
        String right = "Active: " + active;
        fr.drawString("Active: ", px + pw - 14 - fr.getStringWidth(right), fy + 1, SOFT);
        fr.drawString(active, px + pw - 14 - fr.getStringWidth(active), fy + 1, DARK);
    }

    private float ease(float cur, boolean on) {
        float target = on ? 1.0F : 0.0F;
        if (cur < 0.0F) return target;
        return cur + (target - cur) * 0.35F;
    }

    private void drawToggle(int x, int y, String label, float knob, int mx, int my) {
        int w = 22, h = 12;
        int bg = Sky.lerp(0xCFCFCF, 0x3A3A3A, knob) | 0xFF000000;
        Draw.roundRect(x, y, x + w, y + h, 5, bg);
        int kx = x + 2 + (int) ((w - 4 - 8) * knob);
        Draw.roundRect(kx, y + 2, kx + 8, y + 10, 3, 0xFFFFFFFF);
        fontRendererObj.drawString(label, x + w + 6, y + 2, MID);
    }

    // ----------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        layout();
        FontRenderer fr = fontRendererObj;

        // close
        int cbx = px + pw - 28, cby = py + 9;
        if (mx >= cbx && mx < cbx + 18 && my >= cby && my < cby + 18) {
            click();
            mc.displayGuiScreen(null);
            return;
        }

        // chips
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

        // toggles
        int fy = py + ph - 26;
        int tx = px + 14;
        int w1 = 22 + 6 + fr.getStringWidth("Animations");
        if (mx >= tx && mx < tx + w1 && my >= fy && my < fy + 12) {
            SkyFX.animations = !SkyFX.animations;
            SkyFX.save();
            click();
            return;
        }
        int tx2 = tx + 26 + fr.getStringWidth("Animations") + 18;
        int w2 = 22 + 6 + fr.getStringWidth("Sky fog");
        if (mx >= tx2 && mx < tx2 + w2 && my >= fy && my < fy + 12) {
            SkyFX.fogTint = !SkyFX.fogTint;
            SkyFX.save();
            click();
            return;
        }

        // cards
        if (mx >= gx && mx < gx + gw && my >= gy && my < gy + gh) {
            for (int i = 0; i < shown.size(); i++) {
                int x = cardX(i);
                int y = cardY(i);
                if (mx >= x && mx < x + cardW && my >= y && my < y + cardH) {
                    SkyFX.setSky(shown.get(i));
                    click();
                    return;
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            scrollTarget -= Integer.signum(d) * (cardH * 0.6F);
            scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        }
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}
