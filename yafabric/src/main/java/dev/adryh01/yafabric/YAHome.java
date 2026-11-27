package dev.adryh01.yafabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.font.TextRenderer;

import java.io.IOException;

/**
 * The YA home screen on Fabric (stage 1): top bar with tabs and a Play panel, same look as the Forge test home.
 * Only plain rectangles and text are used here so it stays easy to port; the 3D character comes in the next stage.
 */
public class YAHome extends Screen {

    private static final String[] TABS = {"Play", "Servers", "Worlds", "Options"};
    private static final int BG1 = 0xFFF7F7F7, BORDER = 0xFFD6D6D6, DARK = 0xFF2E2E2E, MID = 0xFF6B6B6B, SOFT = 0xFF9A9A9A, WHITE = 0xFFFFFFFF;

    private TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private int tabX(int i) {
        int total = 0;
        for (String s : TABS) total += font().getStringWidth(s) + 28;
        int x = (width - total) / 2;
        for (int k = 0; k < i; k++) x += font().getStringWidth(TABS[k]) + 28;
        return x;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void box(int x1, int y1, int x2, int y2, int border, int fill) {
        DrawableHelper.fill(x1, y1, x2, y2, border);
        DrawableHelper.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    @Override
    public void render(int mx, int my, float delta) {
        TextRenderer fr = font();
        DrawableHelper.fill(0, 0, width, height, 0xFFEAEAEA);
        DrawableHelper.fill(0, 0, width, 34, WHITE);
        DrawableHelper.fill(0, 34, width, 35, BORDER);
        fr.draw("YA CLIENT", 14, 13, DARK);
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i), w = fr.getStringWidth(TABS[i]);
            boolean on = i == 0, hv = in(mx, my, x - 8, 6, w + 16, 24);
            fr.draw(TABS[i], x, 13, on ? DARK : (hv ? MID : SOFT));
            if (on) DrawableHelper.fill(x, 27, x + w, 29, DARK);
        }
        String name = MinecraftClient.getInstance().getSession().getUsername();
        int aw = fr.getStringWidth(name) + 24;
        box(width - aw - 46, 5, width - 46, 29, BORDER, WHITE);
        fr.draw(name, width - aw - 34, 13, DARK);
        boolean qh = in(mx, my, width - 34, 5, 24, 24);
        box(width - 34, 5, width - 10, 29, qh ? MID : BORDER, WHITE);
        fr.draw("x", width - 24, 13, qh ? DARK : MID);

        int lx = 14, ly = height - 142;
        box(lx, ly, lx + 190, ly + 70, BORDER, WHITE);
        fr.draw("Welcome to YA CLIENT", lx + 10, ly + 10, DARK);
        fr.draw("Fabric build (test).", lx + 10, ly + 28, MID);

        int pw = 190, ph = 110, px = width - pw - 14, py = height - ph - 14;
        box(px, py, px + pw, py + ph, BORDER, WHITE);
        fr.draw("Play", px + 12, py + 12, DARK);
        boolean h1 = in(mx, my, px + 8, py + 34, pw - 16, 28);
        box(px + 8, py + 34, px + pw - 8, py + 62, DARK, h1 ? 0xFF000000 : DARK);
        fr.draw("Multiplayer", px + pw / 2 - fr.getStringWidth("Multiplayer") / 2, py + 44, WHITE);
        boolean h2 = in(mx, my, px + 8, py + 70, pw - 16, 28);
        box(px + 8, py + 70, px + pw - 8, py + 98, BORDER, h2 ? 0xFFEDEDED : 0xFFF3F3F3);
        fr.draw("Singleplayer", px + pw / 2 - fr.getStringWidth("Singleplayer") / 2, py + 80, MID);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i), w = font().getStringWidth(TABS[i]);
            if (!in(mx, my, x - 8, 6, w + 16, 24)) continue;
            if (i == 1) mc.openScreen(new MultiplayerScreen(this));
            else if (i == 2) mc.openScreen(new SelectWorldScreen(this));
            else if (i == 3) mc.openScreen(new OptionsScreen(this, mc.options));
            return;
        }
        if (in(mx, my, width - 34, 5, 24, 24)) {
            mc.scheduleStop();
            return;
        }
        int pw = 190, ph = 110, px = width - pw - 14, py = height - ph - 14;
        if (in(mx, my, px + 8, py + 34, pw - 16, 28)) mc.openScreen(new MultiplayerScreen(this));
        else if (in(mx, my, px + 8, py + 70, pw - 16, 28)) mc.openScreen(new SelectWorldScreen(this));
    }

    @Override
    protected void keyPressed(char c, int key) throws IOException {
        // title screen: Esc does nothing
    }
}
