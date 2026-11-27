package dev.adryh01.yafabric.mods;

import dev.adryh01.yafabric.CapeNet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Small menu opened by Right Shift: MODS or COSMETICS. Also holds the cosmetics screen. */
public class YAHub extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int px, py, pw = 230, ph = 112;
    private long openTime;

    @Override
    public void init() {
        openTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(int mx, int my, float pt) {
        float o = Math.min(1.0F, (System.currentTimeMillis() - openTime) / 200.0F);
        o = 1.0F - (1.0F - o) * (1.0F - o);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1.0F - o) * 10.0F);
        DrawableHelper.fill(0, 0, width, height, ((int) (o * 110.0F) << 24));
        UI.roundBox(px, py, px + pw, py + ph, 6, UI.BORDER, UI.BG, 1);
        TextRenderer fr = mc.textRenderer;
        fr.draw("YA CLIENT", px + 14, py + 12, UI.DARK);
        fr.draw("Choose a menu", px + pw - 14 - fr.getStringWidth("Choose a menu"), py + 12, UI.SOFT);
        card(mx, my, px + 14, py + 34, "MODS", "All the modules");
        card(mx, my, px + 14 + (pw - 28) / 2 + 4, py + 34, "COSMETICS", "Your YA cape");
    }

    private void card(int mx, int my, int x, int y, String t, String sub) {
        int w = (pw - 28) / 2 - 4, h = 58;
        boolean hv = in(mx, my, x, y, w, h);
        UI.roundBox(x, y, x + w, y + h, 5, hv ? UI.ACCENT : UI.BORDER, hv ? UI.CARD_HOVER : UI.CARD, 1);
        TextRenderer fr = mc.textRenderer;
        fr.draw(t, x + (w - fr.getStringWidth(t)) / 2, y + 18, UI.DARK);
        fr.draw(sub, x + (w - fr.getStringWidth(sub)) / 2, y + 34, UI.SOFT);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (button != 0) return;
        int w = (pw - 28) / 2 - 4;
        if (in(mx, my, px + 14, py + 34, w, 58)) mc.setScreen(new ModMenu());
        else if (in(mx, my, px + 14 + (pw - 28) / 2 + 4, py + 34, w, 58)) mc.setScreen(new Cosmetics());
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_RSHIFT && UI.shiftGuard()) return;
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT) mc.setScreen(null);
    }

    // ------------------------------------------------------------------ cosmetics

    /** Your cosmetics: one card per cosmetic you own, with a live preview on your character. */
    public static class Cosmetics extends Screen {
        private final MinecraftClient mc = MinecraftClient.getInstance();
        private static final int CARD_H = 96;
        private int px, py, pw = 300, ph = 230;
        private float scroll;
        private int maxScroll;

        @Override
        public boolean shouldPauseGame() {
            return false;
        }

        @Override
        public void removed() {
            Mods.save();
        }

        private Module cape() {
            return Mods.byId("clientcape");
        }

        private boolean owned() {
            return mc.player != null && CapeNet.hasCape(mc.player.getUuid());
        }

        private boolean in(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        @Override
        public void render(int mx, int my, float pt) {
            px = (width - pw) / 2;
            py = (height - ph) / 2;
            DrawableHelper.fill(0, 0, width, height, 0x6E000000);
            UI.roundBox(px, py, px + pw, py + ph, 6, UI.BORDER, UI.BG, 1);
            TextRenderer fr = mc.textRenderer;
            fr.draw("MY COSMETICS", px + 14, py + 12, UI.DARK);
            fr.draw("< Back", px + pw - 14 - fr.getStringWidth("< Back"), py + 12, in(mx, my, px + pw - 50, py + 8, 40, 16) ? UI.DARK : UI.SOFT);
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
                fr.draw(a, px + (pw - fr.getStringWidth(a)) / 2, ly + 60, UI.MID);
                fr.draw(b, px + (pw - fr.getStringWidth(b)) / 2, ly + 74, UI.SOFT);
            }
            UI.scissorOff();
            maxScroll = Math.max(0, y + (int) scroll - ly - lh);
            if (scroll > maxScroll) scroll = maxScroll;
        }

        private void card(int x, int y, int w, int mx, int my) {
            Module c = cape();
            TextRenderer fr = mc.textRenderer;
            UI.roundBox(x, y, x + w, y + CARD_H, 5, UI.BORDER, UI.CARD, 1);
            UI.roundBox(x + 6, y + 6, x + 74, y + CARD_H - 6, 4, UI.BORDER, 0xFFE4E4E4, 1);
            preview(x + 40, y + CARD_H - 12, 30);
            fr.draw("YA Cape", x + 84, y + 10, UI.DARK);
            String eq = c.enabled ? "Equipped" : "Not equipped";
            fr.draw(eq, x + w - 42 - fr.getStringWidth(eq), y + 10, UI.SOFT);
            UI.sw(x + w - 32, y + 8, c.enabled ? 1.0F : 0.0F);
            fr.draw("White 3D cape, black YA logo.", x + 84, y + 26, UI.SOFT);
            opt(x + 84, y + 44, w - 94, "Show my cape", c.setting("mine").b);
            opt(x + 84, y + 66, w - 94, "Show others' cape", c.setting("others").b);
        }

        private void opt(int x, int y, int w, String t, boolean on) {
            UI.roundBox(x, y, x + w, y + 18, 4, UI.BORDER, UI.BG, 1);
            mc.textRenderer.draw(t, x + 8, y + 5, UI.DARK);
            UI.sw(x + w - 28, y + 3, on ? 1.0F : 0.0F);
        }

        /** Your own character seen from behind, turning slowly side to side so the cape edge shows. */
        private void preview(int x, int y, int s) {
            ClientPlayerEntity e = mc.player;
            if (e == null) return;
            float spin = 180.0F + (float) Math.sin(System.currentTimeMillis() / 1100.0D) * 55.0F;
            float rb = e.bodyYaw, prb = e.prevBodyYaw, ry = e.yaw, rp = e.pitch, hy = e.headYaw, phy = e.prevHeadYaw;
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y, 50.0F);
            GL11.glScalef((float) -s, (float) s, (float) s);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            DiffuseLighting.enable();
            GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
            e.bodyYaw = spin;
            e.prevBodyYaw = spin;
            e.yaw = spin;
            e.pitch = 0.0F;
            e.headYaw = spin;
            e.prevHeadYaw = spin;
            EntityRenderDispatcher rm = mc.getEntityRenderManager();
            rm.setYaw(180.0F);
            rm.setRenderShadows(false);
            rm.render(e, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
            rm.setRenderShadows(true);
            e.bodyYaw = rb;
            e.prevBodyYaw = prb;
            e.yaw = ry;
            e.pitch = rp;
            e.prevHeadYaw = phy;
            e.headYaw = hy;
            GL11.glPopMatrix();
            DiffuseLighting.disable();
        }

        @Override
        public void handleMouse() {
            super.handleMouse();
            int d = Mouse.getEventDWheel();
            if (d != 0) scroll = Math.max(0, Math.min(maxScroll, scroll - Integer.signum(d) * 18));
        }

        @Override
        protected void mouseClicked(int mx, int my, int button) {
            if (button != 0) return;
            if (in(mx, my, px + pw - 50, py + 8, 40, 16)) {
                mc.setScreen(new YAHub());
                return;
            }
            if (!owned()) return;
            Module c = cape();
            int lx = px + 14, ly = py + 32, lw = pw - 28, lh = ph - 46;
            if (!in(mx, my, lx, ly, lw, lh)) return;
            int y = ly - (int) scroll;
            if (in(mx, my, lx + lw - 40, y + 4, 36, 20)) c.setEnabled(!c.enabled);
            else if (in(mx, my, lx + 84, y + 44, lw - 94, 18)) c.setting("mine").b = !c.setting("mine").b;
            else if (in(mx, my, lx + 84, y + 66, lw - 94, 18)) c.setting("others").b = !c.setting("others").b;
        }

        @Override
        protected void keyPressed(char ch, int key) {
        if (key == Keyboard.KEY_RSHIFT && UI.shiftGuard()) return;
            if (key == Keyboard.KEY_ESCAPE) mc.setScreen(null);
            else if (key == Keyboard.KEY_RSHIFT) mc.setScreen(new YAHub());
        }
    }
}
