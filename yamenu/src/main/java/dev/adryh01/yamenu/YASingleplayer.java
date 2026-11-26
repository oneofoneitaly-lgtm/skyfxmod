package dev.adryh01.yamenu;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.world.storage.SaveFormatComparator;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

final class YASingleplayer extends YAScreen {

    private static final int ROW = 38;
    private List<SaveFormatComparator> worlds = new ArrayList<SaveFormatComparator>();
    private int sel = -1, scroll;
    private long lastClick;
    private boolean confirmDelete;
    private YButton play, delete;

    YASingleplayer(GuiScreen parent) {
        super(parent, "Singleplayer");
    }

    @Override
    public void initGui() {
        try {
            worlds = mc.getSaveLoader().getSaveList();
            Collections.sort(worlds);
        } catch (Exception e) {
            worlds = new ArrayList<SaveFormatComparator>();
        }
        buttonList.clear();
        int bw = 120, gap = 6, x = width / 2 - (bw * 3 + gap * 2) / 2, y = height - 38;
        buttonList.add(play = new YButton(1, x, y, bw, 22, "Play selected world"));
        buttonList.add(new YButton(2, x + bw + gap, y, bw, 22, "Create new world"));
        buttonList.add(delete = new YButton(3, x + (bw + gap) * 2, y, bw, 22, "Delete"));
        buttonList.add(new YButton(0, 10, y, 60, 22, "Back"));
        update();
    }

    private void update() {
        play.enabled = sel >= 0 && sel < worlds.size();
        delete.enabled = play.enabled;
        delete.displayString = confirmDelete ? "Sure? Click again" : "Delete";
    }

    private int listTop() {
        return 44;
    }

    private int listBottom() {
        return height - 48;
    }

    private void play() {
        if (sel < 0 || sel >= worlds.size()) return;
        SaveFormatComparator w = worlds.get(sel);
        if (w.requiresConversion() || !mc.getSaveLoader().canLoadWorld(w.getFileName())) return;
        mc.displayGuiScreen(null);
        mc.launchIntegratedServer(w.getFileName(), w.getDisplayName(), null);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case 1:
                play();
                break;
            case 2:
                mc.displayGuiScreen(new GuiCreateWorld(this));
                break;
            case 3:
                if (!confirmDelete) {
                    confirmDelete = true;
                } else if (sel >= 0 && sel < worlds.size()) {
                    mc.getSaveLoader().flushCache();
                    mc.getSaveLoader().deleteWorldDirectory(worlds.get(sel).getFileName());
                    confirmDelete = false;
                    sel = -1;
                    initGui();
                    return;
                }
                update();
                break;
            case 0:
                mc.displayGuiScreen(parent);
                break;
            default:
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0 || my < listTop() || my > listBottom()) return;
        int cw = Math.min(width - 40, 420), x = width / 2 - cw / 2;
        if (mx < x || mx > x + cw) return;
        int i = (my - listTop() + scroll) / ROW;
        if (i >= 0 && i < worlds.size()) {
            long now = System.currentTimeMillis();
            if (i == sel && now - lastClick < 400L) {
                play();
                return;
            }
            sel = i;
            lastClick = now;
            confirmDelete = false;
            update();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll = Math.max(0, Math.min(Math.max(0, worlds.size() * ROW - (listBottom() - listTop())), scroll - Integer.signum(d) * ROW));
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        background(true);
        int cw = Math.min(width - 40, 420), x = width / 2 - cw / 2, top = listTop(), bot = listBottom();
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, (height - bot) * f, width * f, (bot - top) * f);
        if (worlds.isEmpty()) {
            fontRendererObj.drawString("No worlds yet. Create one!", width / 2 - fontRendererObj.getStringWidth("No worlds yet. Create one!") / 2, top + 20, UI.SOFT);
        }
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (int i = 0; i < worlds.size(); i++) {
            int y = top + i * ROW - scroll;
            if (y + ROW < top || y > bot) continue;
            SaveFormatComparator w = worlds.get(i);
            boolean over = mx >= x && mx <= x + cw && my >= Math.max(y, top) && my < Math.min(y + ROW - 4, bot);
            UI.box(x, y, x + cw, y + ROW - 4, 6, i == sel ? UI.ACCENT : UI.BORDER, over ? 0xFFFAFAFA : UI.CARD);
            String name = w.getDisplayName();
            if (name == null || name.length() == 0) name = "World " + (i + 1);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(name, cw - 20), x + 10, y + 6, UI.DARK);
            String mode = w.getEnumGameType() == null ? "" : w.getEnumGameType().getName();
            if (w.isHardcoreModeEnabled()) mode = "hardcore";
            String info = w.getFileName() + "  -  " + mode + "  -  " + df.format(new Date(w.getLastTimePlayed()));
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(info, cw - 20), x + 10, y + 19, UI.SOFT);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        super.drawScreen(mx, my, pt);
    }
}
