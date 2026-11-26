package dev.adryh01.skyfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Random;

/**
 * Draws the custom sky on a dome around the camera:
 * gradient (smooth or banded), milky way, stars, grid, aurora, sun/moon/eclipse with rays and halo,
 * drifting cloud puffs, meteors, particles and lightning.
 */
public class SkyRenderer extends IRenderHandler {

    public static final SkyRenderer INSTANCE = new SkyRenderer();

    /** Replaces the vanilla cloud renderer on skies that do not want vanilla clouds. */
    public static final IRenderHandler NO_CLOUDS = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
        }
    };

    /** Must stay below the minimum far plane (2 chunk render distance = 64 blocks). */
    public static final double RADIUS = 50.0D;

    private static final float[] RINGS = {-90, -40, -15, -5, 0, 4, 10, 18, 28, 40, 55, 70, 90};
    private static final int AZ_SEGMENTS = 24;

    private static void normalBlend() {
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
    }

    private static void additiveBlend() {
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        Sky s = SkyFX.current;
        if (s == null || s.vanilla) return;

        float t = Sky.timeSec();
        float k = s.blend(t);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        normalBlend();
        GL11.glShadeModel(GL11.GL_SMOOTH);

        drawDome(s, k);

        if (s.milky != -1 || s.starCount > 0) {
            GlStateManager.pushMatrix();
            if (SkyFX.animations) {
                GlStateManager.rotate(t * 0.35F, 0.3F, 1.0F, 0.2F);
            }
            if (s.milky != -1) drawMilky(s);
            if (s.starCount > 0) drawStars(s, t);
            GlStateManager.popMatrix();
        }

        if (s.grid != -1) drawGrid(s, t);
        if (s.aurora) drawAurora(s, t);
        for (Sky.Body b : s.bodies) drawBody(s, b, t, k);
        if (s.puffs != null) drawPuffs(s, t);
        if (SkyFX.animations) {
            if (s.meteors) drawMeteors(t);
            if (s.lightning) drawLightning(t);
        }
        if (s.particles != null) drawParticles(s, t);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        normalBlend();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableFog();
    }

    // ------------------------------------------------------------ primitives

    private static void v(WorldRenderer wr, double x, double y, double z, int argb) {
        wr.pos(x, y, z).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255).endVertex();
    }

    /** Vertex on the dome at (azimuth deg, elevation deg). */
    private static void pt(WorldRenderer wr, double azDeg, double elDeg, int rgb, int alpha) {
        double az = Math.toRadians(azDeg);
        double el = Math.toRadians(elDeg);
        double ce = Math.cos(el);
        v(wr, RADIUS * ce * Math.sin(az), RADIUS * Math.sin(el), -RADIUS * ce * Math.cos(az), (alpha << 24) | (rgb & 0xFFFFFF));
    }

    private static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    private static double[] direction(double azDeg, double elDeg) {
        double az = Math.toRadians(azDeg);
        double el = Math.toRadians(elDeg);
        double ce = Math.cos(el);
        return new double[]{ce * Math.sin(az), Math.sin(el), -ce * Math.cos(az)};
    }

    /** Basis (right, up) of the tangent plane for a unit direction. */
    private static double[][] basis(double[] f) {
        double ux0 = 0.0D, uy0 = 1.0D, uz0 = 0.0D;
        if (Math.abs(f[1]) > 0.98D) {
            ux0 = 1.0D;
            uy0 = 0.0D;
        }
        double rx = f[1] * uz0 - f[2] * uy0;
        double ry = f[2] * ux0 - f[0] * uz0;
        double rz = f[0] * uy0 - f[1] * ux0;
        double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx /= rl;
        ry /= rl;
        rz /= rl;
        double ux = ry * f[2] - rz * f[1];
        double uy = rz * f[0] - rx * f[2];
        double uz = rx * f[1] - ry * f[0];
        return new double[][]{{rx, ry, rz}, {ux, uy, uz}};
    }

    /** Moves a direction by (dxDeg, dyDeg) along the tangent plane. */
    private static double[] offset(double[] f, double dxDeg, double dyDeg) {
        double[][] b = basis(f);
        double dx = Math.tan(Math.toRadians(dxDeg));
        double dy = Math.tan(Math.toRadians(dyDeg));
        double x = f[0] + b[0][0] * dx + b[1][0] * dy;
        double y = f[1] + b[0][1] * dx + b[1][1] * dy;
        double z = f[2] + b[0][2] * dx + b[1][2] * dy;
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[]{x / l, y / l, z / l};
    }

    /** Appends a (possibly elliptical) radial-gradient blob as triangles to a GL_TRIANGLES batch. */
    private static void blob(WorldRenderer wr, double[] f, double rxDeg, double ryDeg, int inner, int outer, int seg) {
        double[][] b = basis(f);
        double rx = Math.tan(Math.toRadians(Math.min(rxDeg, 80.0D))) * RADIUS;
        double ry = Math.tan(Math.toRadians(Math.min(ryDeg, 80.0D))) * RADIUS;
        double cx = f[0] * RADIUS, cy = f[1] * RADIUS, cz = f[2] * RADIUS;
        double px = 0, py = 0, pz = 0;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2.0D * i / seg;
            double ca = Math.cos(a) * rx;
            double sa = Math.sin(a) * ry;
            double x = cx + b[0][0] * ca + b[1][0] * sa;
            double y = cy + b[0][1] * ca + b[1][1] * sa;
            double z = cz + b[0][2] * ca + b[1][2] * sa;
            if (i > 0) {
                v(wr, cx, cy, cz, inner);
                v(wr, px, py, pz, outer);
                v(wr, x, y, z, outer);
            }
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void discOne(double[] f, double radiusDeg, int inner, int outer) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(4, DefaultVertexFormats.POSITION_COLOR);
        blob(wr, f, radiusDeg, radiusDeg, inner, outer, 32);
        tess.draw();
    }

    // ------------------------------------------------------------------ dome

    private static float[] bandRings(int bands) {
        float step = 90.0F / bands;
        float[] r = new float[bands * 2 + 1];
        for (int i = 0; i < r.length; i++) r[i] = -90.0F + i * step;
        return r;
    }

    private void drawDome(Sky s, float k) {
        float[] rings = s.bands > 0 ? bandRings(s.bands) : RINGS;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < rings.length - 1; i++) {
            float e1 = rings[i];
            float e2 = rings[i + 1];
            int c1, c2;
            if (s.bands > 0) {
                c1 = c2 = s.colorAt((e1 + e2) * 0.5F, k);
            } else {
                c1 = s.colorAt(e1, k);
                c2 = s.colorAt(e2, k);
            }
            for (int j = 0; j < AZ_SEGMENTS; j++) {
                double a1 = 360.0D * j / AZ_SEGMENTS;
                double a2 = 360.0D * (j + 1) / AZ_SEGMENTS;
                pt(wr, a1, e1, c1, 255);
                pt(wr, a2, e1, c1, 255);
                pt(wr, a2, e2, c2, 255);
                pt(wr, a1, e2, c2, 255);
            }
        }
        tess.draw();
    }

    // ----------------------------------------------------------- stars / milky

    private void drawStars(Sky s, float t) {
        float[] d = s.stars();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        int n = d.length / Sky.STAR_STRIDE;
        for (int i = 0; i < n; i++) {
            int o = i * Sky.STAR_STRIDE;
            float tw = SkyFX.animations ? 0.55F + 0.45F * (float) Math.sin(t * 2.0F + d[o + 9]) : 0.85F;
            int a = (int) (255.0F * Math.min(1.0F, d[o + 10] * s.starBright * tw));
            float cx = d[o], cy = d[o + 1], cz = d[o + 2];
            float rx = d[o + 3], ry = d[o + 4], rz = d[o + 5];
            float ux = d[o + 6], uy = d[o + 7], uz = d[o + 8];
            if (s.starPlus) {
                // four-point sparkle: two thin crossed quads
                starQuad(wr, cx, cy, cz, rx, ry, rz, ux * 0.22F, uy * 0.22F, uz * 0.22F, a);
                starQuad(wr, cx, cy, cz, rx * 0.22F, ry * 0.22F, rz * 0.22F, ux, uy, uz, a);
            } else {
                starQuad(wr, cx, cy, cz, rx, ry, rz, ux, uy, uz, a);
            }
        }
        tess.draw();
    }

    private static void starQuad(WorldRenderer wr, float cx, float cy, float cz, float rx, float ry, float rz,
                                 float ux, float uy, float uz, int a) {
        int c = (a << 24) | 0xFFFFFF;
        v(wr, cx - rx - ux, cy - ry - uy, cz - rz - uz, c);
        v(wr, cx + rx - ux, cy + ry - uy, cz + rz - uz, c);
        v(wr, cx + rx + ux, cy + ry + uy, cz + rz + uz, c);
        v(wr, cx - rx + ux, cy - ry + uy, cz - rz + uz, c);
    }

    private void drawMilky(Sky s) {
        additiveBlend();
        float[] d = s.milky();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(4, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < d.length / 5; i++) {
            double[] f = {d[i * 5], d[i * 5 + 1], d[i * 5 + 2]};
            blob(wr, f, d[i * 5 + 3], d[i * 5 + 3], argb((int) d[i * 5 + 4], s.milky), argb(0, s.milky), 8);
        }
        tess.draw();
        normalBlend();
    }

    // ------------------------------------------------------------------ grid

    private void drawGrid(Sky s, float t) {
        additiveBlend();
        int c = s.grid;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 36; i++) {
            double az = i * 10.0D;
            double w = 0.35D;
            pt(wr, az - w, -1.0D, c, 210);
            pt(wr, az + w, -1.0D, c, 210);
            pt(wr, az + w, -70.0D, c, 60);
            pt(wr, az - w, -70.0D, c, 60);
        }
        float frac = SkyFX.animations ? (t * 0.3F) % 1.0F : 0.0F;
        for (int j = 0; j < 10; j++) {
            float d = (j + frac) / 10.0F;
            double e = -1.5D - 66.0D * d * d;
            double th = 0.3D + d * 1.2D;
            int a = (int) (230.0F * (1.0F - d * 0.55F));
            for (int m = 0; m < 48; m++) {
                double a1 = 360.0D * m / 48;
                double a2 = 360.0D * (m + 1) / 48;
                pt(wr, a1, e + th, c, a);
                pt(wr, a2, e + th, c, a);
                pt(wr, a2, e - th, c, a);
                pt(wr, a1, e - th, c, a);
            }
        }
        tess.draw();
        normalBlend();
    }

    // ---------------------------------------------------------------- aurora

    private void drawAurora(Sky s, float t) {
        additiveBlend();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        int steps = 90;
        for (int band = 0; band < 3; band++) {
            for (int i = 0; i < steps; i++) {
                double a1 = Math.PI * 2.0D * i / steps;
                double a2 = Math.PI * 2.0D * (i + 1) / steps;
                auroraColumn(wr, s, a1, a2, band, t);
            }
        }
        tess.draw();
        normalBlend();
    }

    private void auroraColumn(WorldRenderer wr, Sky s, double a1, double a2, int band, float t) {
        double[] eb = new double[2];
        double[] et = new double[2];
        int[] alpha = new int[2];
        int[] rgb = new int[2];
        double[] az = {a1, a2};
        for (int j = 0; j < 2; j++) {
            double a = az[j];
            eb[j] = 14.0D + band * 8.0D + 5.0D * Math.sin(a * 3.0D + t * 0.6D + band);
            et[j] = eb[j] + 18.0D + 8.0D * Math.sin(a * 5.0D - t * 0.9D + band * 2.0D);
            double mod = 0.35D + 0.65D * Math.max(0.0D, Math.sin(a * 1.5D + band * 1.7D + t * 0.2D));
            alpha[j] = (int) (150.0D * mod / (1 + band * 0.4D));
            rgb[j] = Sky.lerp(s.aurora1, s.aurora2, (float) ((Math.sin(a * 2.0D + t * 0.3D) + 1.0D) / 2.0D));
        }
        double d1 = Math.toDegrees(a1), d2 = Math.toDegrees(a2);
        pt(wr, d1, eb[0], rgb[0], alpha[0]);
        pt(wr, d2, eb[1], rgb[1], alpha[1]);
        pt(wr, d2, et[1], rgb[1], 0);
        pt(wr, d1, et[0], rgb[0], 0);
    }

    // ----------------------------------------------------------------- bodies

    private void drawBody(Sky s, Sky.Body b, float t, float k) {
        double[] f = direction(b.az, b.elev);
        int c = b.color;
        double size = b.size;

        if (b.type.equals("eclipse")) {
            discOne(f, size * 3.4D, argb(190, c), argb(0, c));
            discOne(f, size * 1.35D, argb(90, c), argb(0, c));
            discOne(f, size, 0xFF000000, 0xFF000000);
            return;
        }

        boolean crescent = b.type.equals("crescent");
        if (!crescent) {
            discOne(f, size * 6.0D, argb(38, c), argb(0, c));
            discOne(f, size * 2.8D, argb(95, c), argb(0, c));
        }
        if (b.rays > 0) drawRays(f, b, t);
        if (b.halo) ring(f, size * 2.3D, size * 0.14D, argb(110, c));

        discOne(f, size, argb(255, c), argb(255, c));

        if (b.type.equals("moon")) {
            double[][] craters = {{-0.32, 0.25, 0.22}, {0.30, -0.18, 0.16}, {0.05, 0.40, 0.12}};
            for (double[] cr : craters) {
                double[] p = offset(f, cr[0] * size, cr[1] * size);
                discOne(p, cr[2] * size, argb(55, 0x606878), argb(55, 0x606878));
            }
        } else if (crescent) {
            double[] p = offset(f, 0.38D * size, 0.08D * size);
            int cut = 0xFF000000 | s.colorAt(b.elev, k);
            discOne(p, size * 0.86D, cut, cut);
        }
    }

    private void drawRays(double[] f, Sky.Body b, float t) {
        double[][] bs = basis(f);
        int n = b.rays;
        double r0 = b.size * (b.spikes ? 1.05D : 1.2D);
        double len = b.rayLen;
        double w = b.spikes ? r0 * Math.PI / n * 0.9D : r0 * 0.3D;
        double spin = SkyFX.animations ? t * 0.04D : 0.0D;
        int c = b.color;
        int a0 = b.spikes ? 255 : 80;
        int a1 = b.spikes ? 255 : 0;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(4, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            double ang = Math.PI * 2.0D * i / n + spin;
            double[] l = planePoint(f, bs, ang, r0, -w);
            double[] r = planePoint(f, bs, ang, r0, w);
            double[] tip = planePoint(f, bs, ang, r0 + len, 0.0D);
            v(wr, l[0], l[1], l[2], argb(a0, c));
            v(wr, r[0], r[1], r[2], argb(a0, c));
            v(wr, tip[0], tip[1], tip[2], argb(a1, c));
        }
        tess.draw();
    }

    /** Point in the tangent plane: distance dDeg along angle 'ang', sideways sDeg. */
    private static double[] planePoint(double[] f, double[][] bs, double ang, double dDeg, double sDeg) {
        double d = Math.tan(Math.toRadians(Math.min(dDeg, 80.0D))) * RADIUS;
        double sd = Math.tan(Math.toRadians(sDeg)) * RADIUS;
        double ca = Math.cos(ang), sa = Math.sin(ang);
        double ax = bs[0][0] * ca + bs[1][0] * sa, ay = bs[0][1] * ca + bs[1][1] * sa, az = bs[0][2] * ca + bs[1][2] * sa;
        double px = -bs[0][0] * sa + bs[1][0] * ca, py = -bs[0][1] * sa + bs[1][1] * ca, pz = -bs[0][2] * sa + bs[1][2] * ca;
        return new double[]{f[0] * RADIUS + ax * d + px * sd, f[1] * RADIUS + ay * d + py * sd, f[2] * RADIUS + az * d + pz * sd};
    }

    private void ring(double[] f, double radiusDeg, double thickDeg, int color) {
        double[][] bs = basis(f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        int seg = 56;
        for (int i = 0; i < seg; i++) {
            double a1 = Math.PI * 2.0D * i / seg;
            double a2 = Math.PI * 2.0D * (i + 1) / seg;
            double[] p1 = planePoint(f, bs, a1, radiusDeg - thickDeg, 0.0D);
            double[] p2 = planePoint(f, bs, a1, radiusDeg + thickDeg, 0.0D);
            double[] p3 = planePoint(f, bs, a2, radiusDeg + thickDeg, 0.0D);
            double[] p4 = planePoint(f, bs, a2, radiusDeg - thickDeg, 0.0D);
            v(wr, p1[0], p1[1], p1[2], color);
            v(wr, p2[0], p2[1], p2[2], color);
            v(wr, p3[0], p3[1], p3[2], color);
            v(wr, p4[0], p4[1], p4[2], color);
        }
        tess.draw();
    }

    // ------------------------------------------------------------ cloud puffs

    private void drawPuffs(Sky s, float t) {
        Sky.Puffs p = s.puffs;
        List<Sky.Cloud> clouds = s.clouds();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(4, DefaultVertexFormats.POSITION_COLOR);
        for (int layer = 0; layer < 3; layer++) {
            if (layer == 0 && p.outline < 0) continue;
            for (Sky.Cloud c : clouds) {
                double az = c.az + t * c.speed * 0.5D;
                for (int j = 0; j < c.puffs.length; j += 3) {
                    double ox = c.puffs[j];
                    double oy = c.puffs[j + 1];
                    double r = c.puffs[j + 2];
                    double el = c.el + oy;
                    double a = az + ox / Math.max(0.35D, Math.cos(Math.toRadians(el)));
                    double[] f;
                    double rr;
                    int col;
                    int a1 = p.alpha;
                    int a2 = (int) (p.alpha * (1.0F - p.soft));
                    if (layer == 0) {
                        f = direction(a, el);
                        rr = r * 1.1D + 0.35D;
                        col = p.outline;
                        a1 = 255;
                        a2 = 255;
                    } else if (layer == 1) {
                        f = direction(a, el - r * 0.1D);
                        rr = r;
                        col = p.bottom;
                    } else {
                        f = direction(a, el + r * 0.18D);
                        rr = r * 0.8D;
                        col = p.top;
                    }
                    blob(wr, f, rr, rr * p.flat, argb(a1, col), argb(a2, col), 14);
                }
            }
        }
        tess.draw();
    }

    // ---------------------------------------------------------------- meteors

    private void drawMeteors(float t) {
        additiveBlend();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int m = 0; m < 3; m++) {
            float period = 4.0F + m * 1.7F;
            float tt = t + m * 2.3F;
            int idx = (int) (tt / period);
            float ph = tt - idx * period;
            float dur = 0.9F;
            if (ph > dur) continue;
            float p = ph / dur;
            Random r = new Random(idx * 131L + m * 977L);
            double az = r.nextDouble() * 360.0D;
            double el0 = 35.0D + r.nextDouble() * 30.0D;
            double ang = Math.toRadians(20.0D + r.nextDouble() * 40.0D);
            double dAz = Math.cos(ang) * 28.0D;
            double dEl = -Math.sin(ang) * 28.0D;
            double ha = az + dAz * p, he = el0 + dEl * p;
            double ta = ha - dAz * 0.35D, te = he - dEl * 0.35D;
            int a = (int) (255.0F * (1.0F - p * 0.6F));
            double w = 0.25D;
            pt(wr, ha, he + w, 0xFFFFFF, a);
            pt(wr, ha, he - w, 0xFFFFFF, a);
            pt(wr, ta, te - w, 0xFFFFFF, 0);
            pt(wr, ta, te + w, 0xFFFFFF, 0);
        }
        tess.draw();
        normalBlend();
    }

    // -------------------------------------------------------------- particles

    private void drawParticles(Sky s, float t) {
        Sky.Particles pa = s.particles;
        float[] d = s.parts();
        boolean petal = pa.type.equals("petal");
        boolean firefly = pa.type.equals("firefly");
        if (!petal) additiveBlend();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < d.length / 5; i++) {
            double az0 = d[i * 5], el0 = d[i * 5 + 1], sp = d[i * 5 + 2], ph = d[i * 5 + 3], sz = d[i * 5 + 4];
            double az, el;
            double pulse;
            if (petal) {
                el = 58.0D - ((el0 + t * sp * 1.2D) % 55.0D);
                az = az0 + t * sp + Math.sin(t * 0.9D + ph) * 3.0D;
                pulse = 1.0D;
            } else if (firefly) {
                az = az0 + Math.sin(t * 0.3D + ph) * 5.0D;
                el = 6.0D + el0 * 0.4D + Math.sin(t * 0.45D + ph * 2.0D) * 3.0D;
                double q = 0.5D + 0.5D * Math.sin(t * 1.3D + ph);
                pulse = q * q;
            } else {
                el = 3.0D + ((el0 + t * sp * 1.6D) % 55.0D);
                az = az0 + Math.sin(t * 0.5D + ph) * 2.0D;
                pulse = 0.35D + 0.65D * (0.5D + 0.5D * Math.sin(t * 2.2D + ph));
            }
            int a = (int) (235.0D * pulse);
            double ce = Math.max(0.35D, Math.cos(Math.toRadians(el)));
            double sx = sz / ce;
            if (petal) {
                diamond(wr, az, el, sx * 0.8D, sz * 1.3D, pa.color, a);
            } else {
                diamond(wr, az, el, sx * 2.4D, sz * 2.4D, pa.color, a / 4);
                diamond(wr, az, el, sx, sz, pa.color, a);
            }
        }
        tess.draw();
        normalBlend();
    }

    private static void diamond(WorldRenderer wr, double az, double el, double dAz, double dEl, int rgb, int a) {
        pt(wr, az, el + dEl, rgb, a);
        pt(wr, az + dAz, el, rgb, a);
        pt(wr, az, el - dEl, rgb, a);
        pt(wr, az - dAz, el, rgb, a);
    }

    // -------------------------------------------------------------- lightning

    private void drawLightning(float t) {
        float period = 6.5F;
        int idx = (int) (t / period);
        float ph = t - idx * period;
        float in;
        if (ph < 0.12F) in = 1.0F - ph / 0.12F * 0.6F;
        else if (ph < 0.2F) in = 0.15F;
        else if (ph < 0.34F) in = 0.95F * (1.0F - (ph - 0.2F) / 0.14F);
        else return;

        additiveBlend();
        Random r = new Random(idx * 7919L + 3L);
        double az = r.nextDouble() * 360.0D;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

        // soft flash over the whole dome
        int fa = (int) (50.0F * in);
        for (int i = 0; i < RINGS.length - 1; i++) {
            if (RINGS[i + 1] <= -5) continue;
            for (int j = 0; j < AZ_SEGMENTS; j++) {
                double a1 = 360.0D * j / AZ_SEGMENTS;
                double a2 = 360.0D * (j + 1) / AZ_SEGMENTS;
                pt(wr, a1, RINGS[i], 0xCFD8FF, fa);
                pt(wr, a2, RINGS[i], 0xCFD8FF, fa);
                pt(wr, a2, RINGS[i + 1], 0xCFD8FF, fa);
                pt(wr, a1, RINGS[i + 1], 0xCFD8FF, fa);
            }
        }

        // bolt
        int segs = 10;
        double cur = az;
        double eTop = 44.0D, eBot = 3.0D;
        for (int i = 0; i < segs; i++) {
            double e1 = eTop + (eBot - eTop) * i / segs;
            double e2 = eTop + (eBot - eTop) * (i + 1) / segs;
            double nxt = cur + (r.nextDouble() - 0.5D) * 5.0D;
            double w = 0.4D;
            int a = (int) (255.0F * in);
            pt(wr, cur - w, e1, 0xE6F0FF, a);
            pt(wr, cur + w, e1, 0xE6F0FF, a);
            pt(wr, nxt + w, e2, 0xE6F0FF, a);
            pt(wr, nxt - w, e2, 0xE6F0FF, a);
            double g = w * 4.0D;
            pt(wr, cur - g, e1, 0x9DB4FF, a / 4);
            pt(wr, cur + g, e1, 0x9DB4FF, a / 4);
            pt(wr, nxt + g, e2, 0x9DB4FF, a / 4);
            pt(wr, nxt - g, e2, 0x9DB4FF, a / 4);
            cur = nxt;
        }
        tess.draw();
        normalBlend();
    }
}
