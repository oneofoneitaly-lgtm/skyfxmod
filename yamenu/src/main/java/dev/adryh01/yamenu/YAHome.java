package dev.adryh01.yamenu;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * New home screen (test): top bar with tabs, your character in the middle, news/welcome cards on the left,
 * and a Play panel on the right. The original title screen is kept as YAMainMenuClassic.
 */
final class YAHome extends GuiScreen {

    private static final String[] TABS = {"Play", "Servers", "Worlds", "Options"};
    private ResourceLocation skin;
    private boolean slim;
    private ModelPlayer wide, slimModel;
    private boolean multi = true;
    private ServerList servers;
    private long open;

    @Override
    public void initGui() {
        open = System.currentTimeMillis();
        servers = new ServerList(mc);
        servers.loadServerList();
        if (wide == null) {
            wide = new ModelPlayer(0.0F, false);
            slimModel = new ModelPlayer(0.0F, true);
            final GameProfile gp = mc.getSession().getProfile();
            skin = DefaultPlayerSkin.getDefaultSkin(gp.getId());
            slim = "slim".equals(DefaultPlayerSkin.getSkinType(gp.getId()));
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mc.getSessionService().fillProfileProperties(gp, true);
                    } catch (Exception ignored) {
                    }
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            mc.getSkinManager().loadProfileTextures(gp, new SkinManager.SkinAvailableCallback() {
                                @Override
                                public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation loc, MinecraftProfileTexture tex) {
                                    if (type == MinecraftProfileTexture.Type.SKIN) {
                                        skin = loc;
                                        slim = "slim".equals(tex.getMetadata("model"));
                                    }
                                }
                            }, true);
                        }
                    });
                }
            }, "YA Home skin");
            t.setDaemon(true);
            t.start();
        }
    }

    // ------------------------------------------------------------------ layout
    private int tabX(int i) {
        int total = 0;
        for (String s : TABS) total += fontRendererObj.getStringWidth(s) + 28;
        int x = (width - total) / 2;
        for (int k = 0; k < i; k++) x += fontRendererObj.getStringWidth(TABS[k]) + 28;
        return x;
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private ServerData server() {
        return servers != null && servers.countServers() > 0 ? servers.getServerData(0) : null;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        float o = Math.min(1.0F, (System.currentTimeMillis() - open) / 350.0F);
        FontRenderer fr = fontRendererObj;
        // background: soft grey, with a big faint logo
        drawGradientRect(0, 0, width, height, 0xFFF7F7F7, 0xFFDEDEDE);
        UI.logoA(width / 2 - 130, height / 2 - 130, 260, 0.07F);
        // top bar
        Gui.drawRect(0, 0, width, 34, 0xFFFFFFFF);
        Gui.drawRect(0, 34, width, 35, UI.BORDER);
        UI.logo(10, 5, 24);
        fr.drawString("YA CLIENT", 40, 13, UI.DARK);
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i), w = fr.getStringWidth(TABS[i]);
            boolean on = i == 0, hv = in(mx, my, x - 8, 6, w + 16, 24);
            fr.drawString(TABS[i], x, 13, on ? UI.DARK : (hv ? UI.MID : UI.SOFT));
            if (on) Gui.drawRect(x, 27, x + w, 29, UI.DARK);
        }
        // account chip
        String name = mc.getSession().getUsername();
        int aw = fr.getStringWidth(name) + 52;
        int ax = width - aw - 46;
        UI.box(ax, 5, ax + aw, 29, 6, UI.BORDER, 0xFFFFFFFF);
        fr.drawString(name, ax + 10, 13, UI.DARK);
        head(ax + aw - 28, 8, 18);
        boolean qh = in(mx, my, width - 34, 5, 24, 24);
        UI.box(width - 34, 5, width - 10, 29, 6, qh ? UI.ACCENT : UI.BORDER, 0xFFFFFFFF);
        fr.drawString("x", width - 24, 13, qh ? UI.DARK : UI.MID);

        // your character
        int cy = height / 2 + 62;
        String tag = name + "  You";
        int tw = fr.getStringWidth(tag) + 14;
        UI.box(width / 2 - tw / 2, cy - 108, width / 2 + tw / 2, cy - 92, 4, UI.DARK, 0xFFFFFFFF);
        fr.drawString(tag, width / 2 - tw / 2 + 7, cy - 104, UI.DARK);
        model(width / 2, cy, Math.min(86, (height - 100) / 2), mx);

        // left cards
        int lx = 14;
        int ly = height - 142;
        UI.box(lx, ly, lx + 190, ly + 70, 6, UI.BORDER, 0xFFFFFFFF);
        fr.drawString("Welcome to YA CLIENT", lx + 10, ly + 10, UI.DARK);
        fr.drawString("Right Shift in game opens the", lx + 10, ly + 28, UI.MID);
        fr.drawString("YA menu: mods and cosmetics.", lx + 10, ly + 40, UI.MID);
        fr.drawString("Free YA Cape on the store!", lx + 10, ly + 54, UI.SOFT);
        UI.box(lx, height - 62, lx + 190, height - 14, 6, UI.BORDER, 0xFFFFFFFF);
        head(lx + 8, height - 54, 32);
        fr.drawString(name, lx + 48, height - 50, UI.DARK);
        fr.drawString("You", lx + 48, height - 36, UI.SOFT);
        boolean oh = in(mx, my, lx + 142, height - 54, 40, 32);
        UI.box(lx + 142, height - 54, lx + 182, height - 22, 5, oh ? UI.ACCENT : UI.BORDER, 0xFFF3F3F3);
        fr.drawString("Lang", lx + 152, height - 41, UI.MID);

        // play panel
        int pw = 190, ph = 150, px = width - pw - 14, py = height - ph - 14;
        UI.box(px, py, px + pw, py + ph, 8, UI.BORDER, 0xFFFFFFFF);
        int half = (pw - 16) / 2;
        UI.box(px + 8, py + 8, px + 8 + half, py + 28, 5, multi ? UI.BORDER : UI.DARK, multi ? 0xFFF3F3F3 : UI.DARK);
        UI.box(px + 8 + half, py + 8, px + pw - 8, py + 28, 5, multi ? UI.DARK : UI.BORDER, multi ? UI.DARK : 0xFFF3F3F3);
        center("Singleplayer", px + 8 + half / 2, py + 14, multi ? UI.MID : 0xFFFFFFFF);
        center("Multiplayer", px + 8 + half + half / 2, py + 14, multi ? 0xFFFFFFFF : UI.MID);
        ServerData sd = server();
        if (multi) {
            if (sd != null) {
                fr.drawString(trim(sd.serverName, pw - 24), px + 12, py + 42, UI.DARK);
                fr.drawString(trim(sd.serverIP, pw - 24), px + 12, py + 56, UI.SOFT);
            } else {
                fr.drawString("No server saved yet.", px + 12, py + 42, UI.MID);
                fr.drawString("Open Servers to add one.", px + 12, py + 56, UI.SOFT);
            }
        } else {
            fr.drawString("Pick a world and play.", px + 12, py + 42, UI.MID);
        }
        boolean ph2 = in(mx, my, px + 8, py + ph - 64, pw - 16, 28);
        UI.box(px + 8, py + ph - 64, px + pw - 8, py + ph - 36, 6, UI.DARK, ph2 ? 0xFF000000 : UI.DARK);
        center(multi && sd != null ? "Play  " + trim(sd.serverName, 90) : "Play", px + pw / 2, py + ph - 54, 0xFFFFFFFF);
        boolean sh = in(mx, my, px + 8, py + ph - 28, pw - 16, 20);
        UI.box(px + 8, py + ph - 28, px + pw - 8, py + ph - 8, 5, sh ? UI.ACCENT : UI.BORDER, 0xFFF3F3F3);
        center("Classic home", px + pw / 2, py + ph - 22, UI.MID);

        if (o < 1.0F) Gui.drawRect(0, 0, width, height, ((int) ((1.0F - o) * 255.0F) << 24) | 0xFFFFFF);
    }

    private String trim(String s, int w) {
        return fontRendererObj.trimStringToWidth(s == null ? "" : s, w);
    }

    private void center(String s, int cx, int y, int col) {
        fontRendererObj.drawString(s, cx - fontRendererObj.getStringWidth(s) / 2, y, col);
    }

    private void head(int x, int y, int size) {
        if (skin == null) return;
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
    }

    /** Your character (skin loaded from your account), turning slowly and following the mouse a little. */
    private void model(int x, int y, int h, int mx) {
        if (skin == null) return;
        float spin = 180.0F + (float) Math.sin(System.currentTimeMillis() / 2200.0D) * 25.0F + (mx - width / 2) * 0.12F;
        GlStateManager.pushMatrix();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableDepth();
        GlStateManager.translate((float) x, (float) y - h, 60.0F);
        float s = h / 2.0F; // the model is 2 units tall
        GlStateManager.scale(s, s, s);
        GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, -0.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        (slim ? slimModel : wide).render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ input
    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i), w = fontRendererObj.getStringWidth(TABS[i]);
            if (!in(mx, my, x - 8, 6, w + 16, 24)) continue;
            if (i == 1) mc.displayGuiScreen(new net.minecraft.client.gui.GuiMultiplayer(this));
            else if (i == 2) mc.displayGuiScreen(new net.minecraft.client.gui.GuiSelectWorld(this));
            else if (i == 3) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
            return;
        }
        if (in(mx, my, width - 34, 5, 24, 24)) {
            mc.shutdown();
            return;
        }
        int lx = 14;
        if (in(mx, my, lx + 142, height - 54, 40, 32)) {
            mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager()));
            return;
        }
        int pw = 190, ph = 150, px = width - pw - 14, py = height - ph - 14, half = (pw - 16) / 2;
        if (in(mx, my, px + 8, py + 8, half, 20)) multi = false;
        else if (in(mx, my, px + 8 + half, py + 8, half, 20)) multi = true;
        else if (in(mx, my, px + 8, py + ph - 64, pw - 16, 28)) {
            ServerData sd = server();
            if (multi && sd != null) FMLClientHandler.instance().connectToServer(this, sd);
            else if (multi) mc.displayGuiScreen(new net.minecraft.client.gui.GuiMultiplayer(this));
            else mc.displayGuiScreen(new net.minecraft.client.gui.GuiSelectWorld(this));
        } else if (in(mx, my, px + 8, py + ph - 28, pw - 16, 20)) {
            YAMenu.setHomeNew(false);
            mc.displayGuiScreen(new YAMainMenuClassic());
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        // title screen: Esc does nothing
    }
}
