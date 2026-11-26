package dev.adryh01.yamods;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Pixel editor for your own crosshair. Left mouse paints, right mouse erases. */
public class CrosshairEditor extends GuiScreen {

    private static final int N = Fx.Crosshair.N, CELL = 13;
    private final GuiScreen back;
    private final Fx.Crosshair target;
    private int[] cells;
    private int color = 1;
    private int gx, gy, px, py;

    CrosshairEditor(GuiScreen back) {
        this.back = back;
        this.target = (Fx.Crosshair) Mods.byId("crosshair");
    }

    @Override
    public void initGui() {
        cells = target.cells();
        gx = width / 2 - (N * CELL + 24 + 130) / 2;
        gy = height / 2 - N * CELL / 2;
        px = gx + N * CELL + 24;
        py = gy;
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void paint(int mx, int my, int value) {
        if (!in(mx, my, gx, gy, N * CELL, N * CELL)) return;
        cells[((my - gy) / CELL) * N + (mx - gx) / CELL] = value;
    }

    private void save() {
        target.store(cells);
        target.mode.idx = 1;
        Mods.save();
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        paint(mx, my, button == 0 ? color : 0);
        for (int i = 0; i < Fx.Crosshair.PAINT.length; i++) {
            if (in(mx, my, px + (i % 4) * 30, py + 16 + (i / 4) * 30, 24, 24)) color = i;
        }
        int by = py + 16 + 3 * 30 + 8;
        if (button == 0 && in(mx, my, px, by, 118, 18)) cells = new int[N * N];
        if (button == 0 && in(mx, my, px, by + 24, 118, 18)) {
            for (int i = 0; i < cells.length; i++) cells[i] = 0;
            int m = N / 2;
            cells[m * N + m] = color;
        }
        if (button == 0 && in(mx, my, px, by + 48, 118, 18)) {
            for (int i = 0; i < cells.length; i++) cells[i] = 0;
            int m = N / 2;
            for (int i = 2; i <= 5; i++) {
                cells[m * N + m - i] = color;
                cells[m * N + m + i] = color;
                cells[(m - i) * N + m] = color;
                cells[(m + i) * N + m] = color;
            }
        }
        if (button == 0 && in(mx, my, px, by + 78, 118, 20)) {
            save();
            mc.displayGuiScreen(back);
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        paint(mx, my, org.lwjgl.input.Mouse.isButtonDown(1) ? 0 : color);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            save();
            mc.displayGuiScreen(back);
        }
    }

    @Override
    public void onGuiClosed() {
        save();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        Gui.drawRect(0, 0, width, height, 0xFFF3F3F3);
        Gui.drawRect(0, 0, width, 30, UI.HEADER);
        Gui.drawRect(0, 30, width, 31, UI.LINE);
        FontRenderer fr = fontRendererObj;
        fr.drawString("Draw your crosshair", 12, 11, UI.DARK);
        String hint = "Left click = paint   Right click = erase";
        fr.drawString(hint, width - 12 - fr.getStringWidth(hint), 11, UI.SOFT);

        Gui.drawRect(gx - 1, gy - 1, gx + N * CELL + 1, gy + N * CELL + 1, UI.BORDER);
        for (int i = 0; i < N * N; i++) {
            int x = gx + (i % N) * CELL, y = gy + (i / N) * CELL;
            Gui.drawRect(x, y, x + CELL, y + CELL, ((i % N) + (i / N)) % 2 == 0 ? 0xFFFFFFFF : 0xFFF0F0F0);
            if (cells[i] != 0) Gui.drawRect(x, y, x + CELL, y + CELL, 0xFF000000 | Fx.Crosshair.PAINT[cells[i]]);
        }
        int m = N / 2;
        Gui.drawRect(gx + m * CELL, gy, gx + m * CELL + 1, gy + N * CELL, 0x22000000);
        Gui.drawRect(gx, gy + m * CELL, gx + N * CELL, gy + m * CELL + 1, 0x22000000);

        fr.drawString("Colour", px, py + 3, UI.MID);
        for (int i = 0; i < Fx.Crosshair.PAINT.length; i++) {
            int x = px + (i % 4) * 30, y = py + 16 + (i / 4) * 30;
            UI.roundBox(x, y, x + 24, y + 24, 5, i == color ? UI.ACCENT : UI.BORDER, i == 0 ? 0xFFFFFFFF : 0xFF000000 | Fx.Crosshair.PAINT[i], i == color ? 2 : 1);
            if (i == 0) fr.drawString("x", x + 10, y + 8, UI.SOFT);
        }
        int by = py + 16 + 3 * 30 + 8;
        button(mx, my, px, by, 118, 18, "Clear all", false);
        button(mx, my, px, by + 24, 118, 18, "Preset: dot", false);
        button(mx, my, px, by + 48, 118, 18, "Preset: plus", false);
        button(mx, my, px, by + 78, 118, 20, "Save and close", true);

        // real size preview
        int sc = (int) target.scale.v;
        fr.drawString("Preview", gx, gy + N * CELL + 8, UI.MID);
        int pcx = gx + 86, pcy = gy + N * CELL + 12;
        Gui.drawRect(pcx - 20, pcy - 8, pcx + 20 + 0, pcy + 8, 0xFF8FB3A0);
        for (int i = 0; i < N * N; i++) {
            if (cells[i] == 0) continue;
            int x = pcx + (i % N - m) * sc / 2, y = pcy + (i / N - m) * sc / 2;
            int s2 = Math.max(1, sc / 2);
            Gui.drawRect(x, y, x + s2, y + s2, 0xFF000000 | Fx.Crosshair.PAINT[cells[i]]);
        }
    }

    private void button(int mx, int my, int x, int y, int w, int h, String t, boolean primary) {
        boolean over = in(mx, my, x, y, w, h);
        UI.roundRect(x, y, x + w, y + h, 5, primary ? (over ? 0xFF4A4A4A : UI.ACCENT) : (over ? 0xFFD4D4D4 : UI.CHIP));
        fontRendererObj.drawString(t, x + w / 2 - fontRendererObj.getStringWidth(t) / 2, y + (h - 8) / 2, primary ? 0xFFFFFFFF : UI.MID);
    }
}
