package dev.adryh01.skyfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Random;

/** 2D drawing helpers for the menu: rounded rects, gradients, scissor and the live sky previews. */
public final class Draw {

    private Draw() {
    }

    // -------------------------------------------------------------- shapes

    public static void roundRect(int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        r = Math.max(0, Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) {
            Gui.drawRect(x1, y1, x2, y2, color);
            return;
        }
        Gui.drawRect(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5D;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            Gui.drawRect(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            Gui.drawRect(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    public static void roundBox(int x1, int y1, int x2, int y2, int r, int border, int fill, int thick) {
        roundRect(x1, y1, x2, y2, r, border);
        roundRect(x1 + thick, y1 + thick, x2 - thick, y2 - thick, Math.max(0, r - thick), fill);
    }

    /** Vertical gradient; colours are ARGB. */
    public static void gradV(float x1, float y1, float x2, float y2, int top, int bottom) {
        begin2d();
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x2, y1, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, (top >>> 24) & 255).endVertex();
        wr.pos(x1, y1, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, (top >>> 24) & 255).endVertex();
        wr.pos(x1, y2, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, (bottom >>> 24) & 255).endVertex();
        wr.pos(x2, y2, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, (bottom >>> 24) & 255).endVertex();
        t.draw();
        end2d();
    }

    /** Filled circle with a radial gradient (inner -> outer), ARGB. */
    public static void fan(float cx, float cy, float r, int inner, int outer) {
        fanE(cx, cy, r, r, inner, outer);
    }

    /** Filled ellipse with a radial gradient (inner -> outer), ARGB. */
    public static void fanE(float cx, float cy, float rx, float ry, int inner, int outer) {
        begin2d();
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color((inner >> 16) & 255, (inner >> 8) & 255, inner & 255, (inner >>> 24) & 255).endVertex();
        int seg = 28;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2.0D * i / seg;
            wr.pos(cx + Math.cos(a) * rx, cy + Math.sin(a) * ry, 0)
                    .color((outer >> 16) & 255, (outer >> 8) & 255, outer & 255, (outer >>> 24) & 255).endVertex();
        }
        t.draw();
        end2d();
    }

    /** Triangle with per-vertex ARGB colours. */
    public static void tri(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
        begin2d();
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(4, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, 0).color((c1 >> 16) & 255, (c1 >> 8) & 255, c1 & 255, (c1 >>> 24) & 255).endVertex();
        wr.pos(x2, y2, 0).color((c2 >> 16) & 255, (c2 >> 8) & 255, c2 & 255, (c2 >>> 24) & 255).endVertex();
        wr.pos(x3, y3, 0).color((c3 >> 16) & 255, (c3 >> 8) & 255, c3 & 255, (c3 >>> 24) & 255).endVertex();
        t.draw();
        end2d();
    }

    private static void begin2d() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
    }

    private static void end2d() {
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    // ------------------------------------------------------------- scissor

    public static void scissor(Minecraft mc, int x1, int y1, int x2, int y2) {
        int s = new ScaledResolution(mc).getScaleFactor();
        int w = Math.max(0, (x2 - x1) * s);
        int h = Math.max(0, (y2 - y1) * s);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x1 * s, mc.displayHeight - y2 * s, w, h);
    }

    public static void scissorOff() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    // ------------------------------------------------------------ previews

    private static int a(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * Draws a live preview of a sky into the given rectangle (same layers as the 3D renderer).
     * clip = {x1,y1,x2,y2} is the currently active scissor (the scroll area).
     */
    public static void preview(Minecraft mc, Sky s, int x1, int y1, int x2, int y2, float t, int[] clip) {
        int cx1 = Math.max(x1, clip[0]);
        int cy1 = Math.max(y1, clip[1]);
        int cx2 = Math.min(x2, clip[2]);
        int cy2 = Math.min(y2, clip[3]);
        if (cx2 <= cx1 || cy2 <= cy1) return;
        scissor(mc, cx1, cy1, cx2, cy2);

        int w = x2 - x1;
        int h = y2 - y1;
        float hy = y1 + h * 0.78F;
        float topElev = 62.0F;
        float pxDeg = w / 62.0F;
        float k = s.blend(t);

        // 1. gradient (smooth or banded)
        if (s.bands > 0) {
            float step = 90.0F / s.bands;
            for (int i = 0; i * step < topElev; i++) {
                float e1 = i * step, e2 = (i + 1) * step;
                float ya = hy - Math.min(e2, topElev) / topElev * (hy - y1);
                float yb = hy - e1 / topElev * (hy - y1);
                Gui.drawRect(x1, (int) ya, x2, (int) Math.ceil(yb), 0xFF000000 | s.colorAt((e1 + e2) * 0.5F, k));
            }
            Gui.drawRect(x1, (int) hy, x2, y2, 0xFF000000 | s.colorAt(-10, k));
        } else {
            float[] els = {62, 45, 30, 18, 9, 3, 0};
            for (int i = 0; i < els.length - 1; i++) {
                float ya = hy - els[i] / topElev * (hy - y1);
                float yb = hy - els[i + 1] / topElev * (hy - y1);
                gradV(x1, ya, x2, yb, 0xFF000000 | s.colorAt(els[i], k), 0xFF000000 | s.colorAt(els[i + 1], k));
            }
            gradV(x1, hy, x2, y2, 0xFF000000 | s.colorAt(0, k), 0xFF000000 | s.colorAt(-40, k));
        }

        // 2. milky way
        if (s.milky != -1) {
            Random r = new Random(5L);
            for (int i = 0; i < 26; i++) {
                float u = i / 25.0F;
                float mx = x1 + w * (0.05F + 0.9F * u) + (r.nextFloat() - 0.5F) * w * 0.08F;
                float my = hy - h * 0.1F - (hy - y1 - h * 0.15F) * u * 0.9F + (r.nextFloat() - 0.5F) * h * 0.07F;
                fan(mx, my, h * 0.16F, a(26, s.milky), a(0, s.milky));
            }
        }

        // 3. stars
        if (s.starCount > 0) {
            Random r = new Random(s.id.hashCode() + 11L);
            int n = Math.min(36, Math.max(8, s.starCount / 16));
            for (int i = 0; i < n; i++) {
                int sx = (int) (x1 + r.nextFloat() * w);
                int sy = (int) (y1 + r.nextFloat() * (hy - y1 - 3));
                float ph = r.nextFloat() * 6.28F;
                float br = (0.45F + r.nextFloat() * 0.55F) * s.starBright;
                float tw = SkyFX.animations ? 0.55F + 0.45F * (float) Math.sin(t * 2.0F + ph) : 0.85F;
                int al = (int) (255 * Math.min(1.0F, br * tw));
                if (s.starPlus) {
                    Gui.drawRect(sx, sy, sx + 1, sy + 1, a(al, 0xFFFFFF));
                    Gui.drawRect(sx - 1, sy, sx, sy + 1, a(al * 2 / 3, 0xFFFFFF));
                    Gui.drawRect(sx + 1, sy, sx + 2, sy + 1, a(al * 2 / 3, 0xFFFFFF));
                    Gui.drawRect(sx, sy - 1, sx + 1, sy, a(al * 2 / 3, 0xFFFFFF));
                    Gui.drawRect(sx, sy + 1, sx + 1, sy + 2, a(al * 2 / 3, 0xFFFFFF));
                } else {
                    int sz = r.nextFloat() > 0.88F ? 2 : 1;
                    Gui.drawRect(sx, sy, sx + sz, sy + sz, a(al, 0xFFFFFF));
                }
            }
        }

        // 4. aurora ribbons
        if (s.aurora) {
            for (int band = 0; band < 2; band++) {
                for (int x = 0; x < w; x += 2) {
                    float yc = y1 + h * (0.34F + 0.1F * band)
                            + (float) Math.sin(x * 0.09F + t * 0.8F + band * 2.0F) * h * 0.05F;
                    int c = Sky.lerp(s.aurora1, s.aurora2, x / (float) w);
                    float mod = 0.45F + 0.55F * (float) Math.max(0, Math.sin(x * 0.045F + band * 1.7F + t * 0.3F));
                    gradV(x1 + x, yc - h * 0.22F, x1 + x + 2, yc, a(0, c), a((int) (150 * mod), c));
                }
            }
        }

        // 5. sun / moon / eclipse
        for (Sky.Body b : s.bodies) {
            float bx = x1 + w * (0.5F + (float) Math.sin(Math.toRadians(b.az)) * 0.28F);
            float by = hy - b.elev / topElev * (hy - y1);
            float r = Math.max(2.5F, b.size * pxDeg);
            int c = b.color;
            if (b.type.equals("eclipse")) {
                fan(bx, by, r * 3.4F, a(190, c), a(0, c));
                fan(bx, by, r, 0xFF000000, 0xFF000000);
                continue;
            }
            boolean crescent = b.type.equals("crescent");
            if (!crescent) {
                fan(bx, by, r * 5.0F, a(38, c), a(0, c));
                fan(bx, by, r * 2.6F, a(100, c), a(0, c));
            }
            if (b.rays > 0) {
                float r0 = r * (b.spikes ? 1.05F : 1.2F);
                float len = b.rayLen * pxDeg * 0.55F;
                float hw = b.spikes ? (float) (r0 * Math.PI / b.rays * 0.9F) : r0 * 0.3F;
                float spin = SkyFX.animations ? t * 0.04F : 0.0F;
                int a0 = b.spikes ? 255 : 80;
                int a1 = b.spikes ? 255 : 0;
                for (int i = 0; i < b.rays; i++) {
                    float ang = (float) (Math.PI * 2.0D * i / b.rays) + spin;
                    float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
                    float px = -dy, py = dx;
                    tri(bx + dx * r0 + px * hw, by + dy * r0 + py * hw, a(a0, c),
                            bx + dx * r0 - px * hw, by + dy * r0 - py * hw, a(a0, c),
                            bx + dx * (r0 + len), by + dy * (r0 + len), a(a1, c));
                }
            }
            if (b.halo) {
                float hr = r * 2.3F;
                float th = Math.max(1.0F, r * 0.28F);
                int seg = 36;
                for (int i = 0; i < seg; i++) {
                    float a1 = (float) (Math.PI * 2.0D * i / seg), a2 = (float) (Math.PI * 2.0D * (i + 1) / seg);
                    int hc = a(110, c);
                    tri(bx + (float) Math.cos(a1) * (hr - th / 2), by + (float) Math.sin(a1) * (hr - th / 2), hc,
                            bx + (float) Math.cos(a1) * (hr + th / 2), by + (float) Math.sin(a1) * (hr + th / 2), hc,
                            bx + (float) Math.cos(a2) * (hr + th / 2), by + (float) Math.sin(a2) * (hr + th / 2), hc);
                    tri(bx + (float) Math.cos(a1) * (hr - th / 2), by + (float) Math.sin(a1) * (hr - th / 2), hc,
                            bx + (float) Math.cos(a2) * (hr + th / 2), by + (float) Math.sin(a2) * (hr + th / 2), hc,
                            bx + (float) Math.cos(a2) * (hr - th / 2), by + (float) Math.sin(a2) * (hr - th / 2), hc);
                }
            }
            fan(bx, by, r, 0xFF000000 | c, 0xFF000000 | c);
            if (crescent) {
                int cut = 0xFF000000 | s.colorAt(b.elev, k);
                fan(bx + r * 0.38F, by - r * 0.08F, r * 0.86F, cut, cut);
            }
        }

        // 6. cloud puffs (3 layers: outline, shade, light)
        if (s.puffs != null) {
            Sky.Puffs p = s.puffs;
            List<Sky.Cloud> clouds = s.clouds();
            int cn = Math.min(5, clouds.size());
            for (int layer = 0; layer < 3; layer++) {
                if (layer == 0 && p.outline < 0) continue;
                for (int ci = 0; ci < cn; ci++) {
                    Sky.Cloud c = clouds.get(ci);
                    float cx = x1 + (((c.az / 360.0F) + t * 0.012F * c.speed) % 1.0F) * w * 1.5F - w * 0.25F;
                    float cy = hy - Math.min(c.el, 50.0F) / topElev * (hy - y1);
                    for (int j = 0; j < c.puffs.length; j += 3) {
                        float px = cx + c.puffs[j] * pxDeg * 0.9F;
                        float py = cy - c.puffs[j + 1] * pxDeg * 0.9F;
                        float rr = c.puffs[j + 2] * pxDeg * 0.9F;
                        int a1 = p.alpha, a2 = (int) (p.alpha * (1.0F - p.soft));
                        if (layer == 0) {
                            fanE(px, py, rr * 1.1F + 0.6F, (rr * 1.1F + 0.6F) * p.flat, a(255, p.outline), a(255, p.outline));
                        } else if (layer == 1) {
                            fanE(px, py + rr * 0.1F, rr, rr * p.flat, a(a1, p.bottom), a(a2, p.bottom));
                        } else {
                            fanE(px, py - rr * 0.18F, rr * 0.8F, rr * 0.8F * p.flat, a(a1, p.top), a(a2, p.top));
                        }
                    }
                }
            }
        }

        // 7. synthwave grid
        if (s.grid != -1) {
            int gc = s.grid;
            float frac = SkyFX.animations ? (t * 0.3F) % 1.0F : 0.0F;
            Gui.drawRect(x1, (int) hy, x2, y2, a(255, Sky.lerp(s.horizon, s.bottom, 0.8F)));
            for (int j = 0; j < 9; j++) {
                float d = (j + frac) / 9.0F;
                int gy = (int) (hy + (y2 - hy) * d * d);
                Gui.drawRect(x1, gy, x2, gy + 1, a((int) (200 * (1.0F - d * 0.5F)), gc));
            }
            float mx = x1 + w / 2.0F;
            for (int m = -6; m <= 6; m++) {
                float xb = mx + m * w / 5.0F;
                for (int yy = (int) hy; yy < y2; yy++) {
                    float fr = (yy - hy) / (y2 - hy);
                    int gx = (int) (mx + (xb - mx) * fr);
                    Gui.drawRect(gx, yy, gx + 1, yy + 1, a((int) (60 + 140 * (1.0F - fr)), gc));
                }
            }
        }

        // 8. meteors
        if (s.meteors) {
            for (int m = 0; m < 2; m++) {
                float period = 3.2F + m * 1.3F;
                float ph = ((t + m * 1.7F) % period) / 0.9F;
                if (SkyFX.animations ? ph > 1.0F : false) continue;
                if (!SkyFX.animations) ph = 0.5F;
                float hx = x1 + w * (0.25F + 0.35F * m) + ph * w * 0.35F;
                float hyy = y1 + h * (0.1F + 0.2F * m) + ph * h * 0.3F;
                tri(hx, hyy - 0.6F, a(255, 0xFFFFFF), hx, hyy + 0.6F, a(255, 0xFFFFFF),
                        hx - w * 0.14F, hyy - h * 0.09F, a(0, 0xFFFFFF));
            }
        }

        // 9. particles
        if (s.particles != null) {
            Sky.Particles pa = s.particles;
            float[] d = s.parts();
            int n = Math.min(16, d.length / 5);
            boolean petal = pa.type.equals("petal"), firefly = pa.type.equals("firefly");
            for (int i = 0; i < n; i++) {
                float az = d[i * 5], el = d[i * 5 + 1], sp = d[i * 5 + 2], ph = d[i * 5 + 3];
                float px = x1 + (az / 360.0F) * w;
                float py, pulse = 1.0F;
                if (petal) {
                    py = y1 + (((el / 62.0F) + t * 0.04F * sp) % 1.0F) * (hy - y1);
                    px += (float) Math.sin(t + ph) * 3.0F;
                } else if (firefly) {
                    px += (float) Math.sin(t * 0.6F + ph) * 4.0F;
                    py = hy - (el * 0.4F + 6.0F) / 62.0F * (hy - y1) + (float) Math.sin(t * 0.8F + ph) * 3.0F;
                    float q = 0.5F + 0.5F * (float) Math.sin(t * 1.3F + ph);
                    pulse = q * q;
                } else {
                    py = hy - (((el / 62.0F) + t * 0.05F * sp) % 1.0F) * (hy - y1);
                    pulse = 0.35F + 0.65F * (0.5F + 0.5F * (float) Math.sin(t * 2.2F + ph));
                }
                int al = (int) (235 * pulse);
                if (petal) {
                    fanE(px, py, 1.6F, 1.0F, a(al, pa.color), a(al, pa.color));
                } else {
                    fan(px, py, 3.2F, a(al / 3, pa.color), a(0, pa.color));
                    Gui.drawRect((int) px, (int) py, (int) px + 1, (int) py + 1, a(al, 0xFFFFFF));
                }
            }
        }

        // 10. lightning
        if (s.lightning) {
            float ph = t % 6.5F;
            float in = ph < 0.12F ? 1.0F - ph / 0.12F * 0.6F : (ph < 0.2F ? 0.15F : (ph < 0.34F ? 0.95F * (1.0F - (ph - 0.2F) / 0.14F) : 0.0F));
            if (in > 0.0F) {
                Gui.drawRect(x1, y1, x2, (int) hy, a((int) (45 * in), 0xCFD8FF));
                float bx = x1 + w * 0.62F;
                Random r = new Random(4L);
                float yy = y1 + h * 0.08F;
                while (yy < hy - 2) {
                    float nx = bx + (r.nextFloat() - 0.5F) * 6.0F;
                    int len = 5;
                    for (int q = 0; q < len; q++) {
                        int px = (int) (bx + (nx - bx) * q / len);
                        Gui.drawRect(px, (int) (yy + q), px + 1, (int) (yy + q) + 1, a((int) (255 * in), 0xE6F0FF));
                    }
                    bx = nx;
                    yy += len;
                }
            }
        }

        // 11. ground silhouette (not for the grid skies)
        if (s.grid == -1) {
            int ground = Sky.lerp(Sky.lerp(s.horizon, s.bottom, 0.5F), 0x000000, 0.62F);
            for (int x = 0; x < w; x += 2) {
                float hh = h * 0.07F + ((float) Math.sin(x * 0.11F) * 0.5F + 0.5F) * h * 0.09F
                        + ((float) Math.sin(x * 0.037F + 1.0F) * 0.5F + 0.5F) * h * 0.07F;
                Gui.drawRect(x1 + x, (int) (hy - hh + h * 0.04F), Math.min(x2, x1 + x + 2), y2, 0xFF000000 | ground);
            }
        }

        scissor(mc, clip[0], clip[1], clip[2], clip[3]);
    }
}
