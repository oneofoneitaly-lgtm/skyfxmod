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

    public static class Cosmetics extends GuiScreen {
        private int px, py, pw = 330, ph = 210;
        private long openTime;

        @Override
        public void initGui() {
            openTime = System.currentTimeMillis();
        }

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

        @Override
        public void drawScreen(int mx, int my, float pt) {
            px = (width - pw) / 2;
            py = (height - ph) / 2;
            drawRect(0, 0, width, height, 0x6E000000);
            UI.roundBox(px, py, px + pw, py + ph, 6, UI.BORDER, UI.BG, 1);
            FontRenderer fr = fontRendererObj;
            fr.drawString("COSMETICS", px + 14, py + 12, UI.DARK);
            fr.drawString("< Back", px + pw - 14 - fr.getStringWidth("< Back"), py + 12, in(mx, my, px + pw - 50, py + 8, 40, 16) ? UI.DARK : UI.SOFT);
            // preview
            UI.roundBox(px + 14, py + 34, px + 134, py + ph - 14, 5, UI.BORDER, 0xFFE4E4E4, 1);
            preview(px + 74, py + ph - 24, 58);
            // column of every cosmetic, scrollable
            int lx = px + 146, ly = py + 34, lw = pw - 160, lh = ph - 48;
            UI.scissor(mc, lx - 2, ly, lx + lw + 2, ly + lh);
            int y = ly - (int) scroll;
            ClientCape c = cape();
            y = head(lx, y, lw, "YA Cape", c.enabled ? "Equipped" : "Off", c.enabled);
            y = sub(lx, y, lw, "Show my cape", c.own.b);
            y = sub(lx, y, lw, "Show other players' cape", c.others.b);
            y += 6;
            y = soon(lx, y, lw, "Wings");
            y = soon(lx, y, lw, "Bandanas");
            y = soon(lx, y, lw, "Pets");
            content = y + (int) scroll - ly;
            UI.scissorOff();
            maxScroll = Math.max(0, content - lh);
            if (scroll > maxScroll) scroll = maxScroll;
        }

        private float scroll;
        private int content, maxScroll;

        // layout helpers: each returns the next y
        private int head(int x, int y, int w, String t, String st, boolean on) {
            UI.roundBox(x, y, x + w, y + 28, 4, UI.BORDER, UI.CARD, 1);
            fontRendererObj.drawString(t, x + 10, y + 10, UI.DARK);
            fontRendererObj.drawString(st, x + w - 46 - fontRendererObj.getStringWidth(st), y + 10, UI.SOFT);
            UI.sw(x + w - 32, y + 8, on ? 1.0F : 0.0F);
            return y + 32;
        }

        private int sub(int x, int y, int w, String t, boolean on) {
            UI.roundBox(x + 10, y, x + w, y + 20, 4, UI.BORDER, UI.CARD, 1);
            fontRendererObj.drawString(t, x + 20, y + 6, UI.DARK);
            UI.sw(x + w - 32, y + 4, on ? 1.0F : 0.0F);
            return y + 24;
        }

        private int soon(int x, int y, int w, String t) {
            UI.roundBox(x, y, x + w, y + 28, 4, UI.BORDER, 0xFFEDEDED, 1);
            fontRendererObj.drawString(t, x + 10, y + 10, UI.SOFT);
            fontRendererObj.drawString("Coming soon", x + w - 10 - fontRendererObj.getStringWidth("Coming soon"), y + 10, UI.SOFT);
            return y + 32;
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            int d = org.lwjgl.input.Mouse.getEventDWheel();
            if (d != 0) scroll = Math.max(0, Math.min(maxScroll, scroll - Integer.signum(d) * 18));
        }
        private boolean in(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        /** Your own character, seen from the back, slowly turning side to side so the edge is visible. */
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
            GlStateManager.translate(0.0F, 0.0F, 0.0F);
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
        protected void mouseClicked(int mx, int my, int button) throws IOException {
            super.mouseClicked(mx, my, button);
            if (button != 0) return;
            if (in(mx, my, px + pw - 50, py + 8, 40, 16)) {
                mc.displayGuiScreen(new YAHub());
                return;
            }
            ClientCape c = cape();
            int lx = px + 146, ly = py + 34, lw = pw - 160, lh = ph - 48;
            if (!in(mx, my, lx, ly, lw, lh)) return;
            int y = ly - (int) scroll;
            if (in(mx, my, lx, y, lw, 28)) c.setEnabled(!c.enabled);
            else if (in(mx, my, lx + 10, y + 32, lw - 10, 20)) c.own.b = !c.own.b;
            else if (in(mx, my, lx + 10, y + 56, lw - 10, 20)) c.others.b = !c.others.b;
        }

        @Override
        protected void keyTyped(char ch, int key) throws IOException {
            if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
            else if (key == YAMods.menuKey.getKeyCode()) mc.displayGuiScreen(new YAHub());
        }
    }
}
