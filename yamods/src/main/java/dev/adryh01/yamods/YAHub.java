package dev.adryh01.yamods;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.entity.EntityPlayerSP;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Small menu opened by Right Shift: MODS or COSMETICS. Also holds the cosmetics screen. */
public class YAHub extends GuiScreen {

    private int px, py, pw = 230, ph = 112;
    private long openTime;

    @Override
    public void initGui() {
        openTime = System.currentTimeMillis();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        float o = Math.min(1.0F, (System.currentTimeMillis() - openTime) / 200.0F);
        o = 1.0F - (1.0F - o) * (1.0F - o);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1.0F - o) * 10.0F);
        drawRect(0, 0, width, height, ((int) (o * 110.0F) << 24));
        UI.roundBox(px, py, px + pw, py + ph, 6, UI.BORDER, UI.BG, 1);
        FontRenderer fr = fontRendererObj;
        fr.drawString("YA CLIENT", px + 14, py + 12, UI.DARK);
        fr.drawString("Choose a menu", px + pw - 14 - fr.getStringWidth("Choose a menu"), py + 12, UI.SOFT);
        card(mx, my, px + 14, py + 34, "MODS", "All the modules");
        card(mx, my, px + 14 + (pw - 28) / 2 + 4, py + 34, "COSMETICS", "Your YA cape");
    }

    private void card(int mx, int my, int x, int y, String t, String sub) {
        int w = (pw - 28) / 2 - 4, h = 58;
        boolean hv = in(mx, my, x, y, w, h);
        UI.roundBox(x, y, x + w, y + h, 5, hv ? UI.ACCENT : UI.BORDER, hv ? UI.CARD_HOVER : UI.CARD, 1);
        FontRenderer fr = fontRendererObj;
        fr.drawString(t, x + (w - fr.getStringWidth(t)) / 2, y + 18, UI.DARK);
        fr.drawString(sub, x + (w - fr.getStringWidth(sub)) / 2, y + 34, UI.SOFT);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        int w = (pw - 28) / 2 - 4;
        if (in(mx, my, px + 14, py + 34, w, 58)) mc.displayGuiScreen(new ModMenu());
        else if (in(mx, my, px + 14 + (pw - 28) / 2 + 4, py + 34, w, 58)) mc.displayGuiScreen(new Cosmetics());
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE || key == YAMods.menuKey.getKeyCode()) mc.displayGuiScreen(null);
    }

    // ------------------------------------------------------------------ cosmetics

    /** Your cosmetics: one card per cosmetic you own, with a live preview on your character. */
    public static class Cosmetics extends GuiScreen {
        private static final int CARD_H = 96;
        private int px, py, pw = 300, ph = 230;
        private float scroll;
        private int maxScroll;

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void onGuiClosed() {
            Mods.save();
        }

        private ClientCape cape() {
            return (ClientCape) Mods.byId("clientcape");
        }

        private boolean owned() {
            return mc.thePlayer != null && YaNet.hasCape(mc.thePlayer.getUniqueID());
        }

        private boolean in(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            px = (width - pw) / 2;
            py = (height - ph) / 2;
            drawRect(0, 0, width, height, 0x6E000000);
            UI.roundBox(px, py, px + pw, py + ph, 6, UI.BORDER, UI.BG, 1);
            FontRenderer fr = fontRendererObj;
            fr.drawString("MY COSMETICS", px + 14, py + 12, UI.DARK);
            fr.drawString("< Back", px + pw - 14 - fr.getStringWidth("< Back"), py + 12, in(mx, my, px + pw - 50, py + 8, 40, 16) ? UI.DARK : UI.SOFT);
            int lx = px + 14, ly = py + 32, lw = pw - 28, lh = ph - 46;
            UI.scissor(mc, lx - 2, ly, lx + lw + 2, ly + lh);
            int y = ly - (int) scroll;
            int count = 0;
            if (owned()) {
                count = 1;
                card(lx, y, lw, mx, my);
                y += CARD_H + 6;
            }
            if (count == 0) {
                String a = "You do not own any cosmetic yet.";
                String b = "Get the free YA Cape on the store.";
                fr.drawString(a, px + (pw - fr.getStringWidth(a)) / 2, ly + 60, UI.MID);
                fr.drawString(b, px + (pw - fr.getStringWidth(b)) / 2, ly + 74, UI.SOFT);
            }
            UI.scissorOff();
            maxScroll = Math.max(0, y + (int) scroll - ly - lh);
            if (scroll > maxScroll) scroll = maxScroll;
        }

        private void card(int x, int y, int w, int mx, int my) {
            ClientCape c = cape();
            FontRenderer fr = fontRendererObj;
            UI.roundBox(x, y, x + w, y + CARD_H, 5, UI.BORDER, UI.CARD, 1);
            UI.roundBox(x + 6, y + 6, x + 74, y + CARD_H - 6, 4, UI.BORDER, 0xFFE4E4E4, 1);
            preview(x + 40, y + CARD_H - 12, 30);
            fr.drawString("YA Cape", x + 84, y + 10, UI.DARK);
            fr.drawString(c.enabled ? "Equipped" : "Not equipped", x + w - 42 - fr.getStringWidth(c.enabled ? "Equipped" : "Not equipped"), y + 10, UI.SOFT);
            UI.sw(x + w - 32, y + 8, c.enabled ? 1.0F : 0.0F);
            fr.drawString("White 3D cape, black YA logo.", x + 84, y + 26, UI.SOFT);
            opt(x + 84, y + 44, w - 94, "Show my cape", c.own.b);
            opt(x + 84, y + 66, w - 94, "Show others' cape", c.others.b);
        }

        private void opt(int x, int y, int w, String t, boolean on) {
            UI.roundBox(x, y, x + w, y + 18, 4, UI.BORDER, UI.BG, 1);
            fontRendererObj.drawString(t, x + 8, y + 5, UI.DARK);
            UI.sw(x + w - 28, y + 3, on ? 1.0F : 0.0F);
        }

        /** Your own character seen from behind, turning slowly side to side so the cape edge shows. */
        private void preview(int x, int y, int s) {
            EntityPlayerSP e = mc.thePlayer;
            if (e == null) return;
            float spin = 180.0F + (float) Math.sin(System.currentTimeMillis() / 1100.0D) * 55.0F;
            float rb = e.renderYawOffset, ry = e.rotationYaw, rp = e.rotationPitch, pyaw = e.prevRotationYawHead, ryh = e.rotationYawHead, prb = e.prevRenderYawOffset;
            GlStateManager.enableColorMaterial();
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, 50.0F);
            GlStateManager.scale((float) -s, (float) s, (float) s);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            e.renderYawOffset = spin;
            e.prevRenderYawOffset = spin;
            e.rotationYaw = spin;
            e.rotationPitch = 0.0F;
            e.rotationYawHead = spin;
            e.prevRotationYawHead = spin;
            RenderManager rm = mc.getRenderManager();
            rm.setPlayerViewY(180.0F);
            rm.setRenderShadow(false);
            rm.renderEntityWithPosYaw(e, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
            rm.setRenderShadow(true);
            e.renderYawOffset = rb;
            e.prevRenderYawOffset = prb;
            e.rotationYaw = ry;
            e.rotationPitch = rp;
            e.prevRotationYawHead = pyaw;
            e.rotationYawHead = ryh;
            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            int d = org.lwjgl.input.Mouse.getEventDWheel();
            if (d != 0) scroll = Math.max(0, Math.min(maxScroll, scroll - Integer.signum(d) * 18));
        }

        @Override
        protected void mouseClicked(int mx, int my, int button) throws IOException {
            super.mouseClicked(mx, my, button);
            if (button != 0) return;
            if (in(mx, my, px + pw - 50, py + 8, 40, 16)) {
                mc.displayGuiScreen(new YAHub());
                return;
            }
            if (!owned()) return;
            ClientCape c = cape();
            int lx = px + 14, ly = py + 32, lw = pw - 28, lh = ph - 46;
            if (!in(mx, my, lx, ly, lw, lh)) return;
            int y = ly - (int) scroll;
            if (in(mx, my, lx + lw - 40, y + 4, 36, 20)) c.setEnabled(!c.enabled);
            else if (in(mx, my, lx + 84, y + 44, lw - 94, 18)) c.own.b = !c.own.b;
            else if (in(mx, my, lx + 84, y + 66, lw - 94, 18)) c.others.b = !c.others.b;
        }

        @Override
        protected void keyTyped(char ch, int key) throws IOException {
            if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
            else if (key == YAMods.menuKey.getKeyCode()) mc.displayGuiScreen(new YAHub());
        }
    }
}
