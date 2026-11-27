package dev.adryh01.yafabric.mods;

import net.minecraft.util.math.Box;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;

/** Small raw-GL helpers. Every state we change is restored to what it was, so the game's own state stays valid. */
final class Gl {

    private Gl() {
    }

    private static final FloatBuffer BUF = BufferUtils.createFloatBuffer(16);
    private static final ArrayDeque<float[]> COLORS = new ArrayDeque<float[]>();

    /** Remembers the GL state we are about to change. Every save() needs a restore(). */
    static int save() {
        BUF.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, BUF);
        COLORS.push(new float[]{BUF.get(0), BUF.get(1), BUF.get(2), BUF.get(3)});
        int s = 0;
        if (GL11.glIsEnabled(GL11.GL_BLEND)) s |= 1;
        if (GL11.glIsEnabled(GL11.GL_LIGHTING)) s |= 2;
        if (GL11.glIsEnabled(GL11.GL_DEPTH_TEST)) s |= 4;
        if (GL11.glIsEnabled(GL11.GL_TEXTURE_2D)) s |= 8;
        if (GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)) s |= 16;
        if (GL11.glIsEnabled(GL11.GL_ALPHA_TEST)) s |= 32;
        return s;
    }

    private static void set(int cap, boolean on) {
        if (on) GL11.glEnable(cap);
        else GL11.glDisable(cap);
    }

    static void restore(int s) {
        set(GL11.GL_BLEND, (s & 1) != 0);
        set(GL11.GL_LIGHTING, (s & 2) != 0);
        set(GL11.GL_DEPTH_TEST, (s & 4) != 0);
        set(GL11.GL_TEXTURE_2D, (s & 8) != 0);
        GL11.glDepthMask((s & 16) != 0);
        set(GL11.GL_ALPHA_TEST, (s & 32) != 0);
        GL11.glLineWidth(1.0F);
        // put the colour back exactly as it was: the game caches the current colour and must stay in sync
        float[] c = COLORS.isEmpty() ? new float[]{1.0F, 1.0F, 1.0F, 1.0F} : COLORS.pop();
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
    }

    /** Flat-colour drawing: no texture, no lighting, normal alpha blending. */
    static void flat() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    static int r(int rgb) {
        return (rgb >> 16) & 255;
    }

    static int g(int rgb) {
        return (rgb >> 8) & 255;
    }

    static int b(int rgb) {
        return rgb & 255;
    }

    /** Filled rectangle in the current (2D or billboard) space; argb colour. */
    static void rect(float x1, float y1, float x2, float y2, int argb) {
        GL11.glColor4f(((argb >> 16) & 255) / 255.0F, ((argb >> 8) & 255) / 255.0F, (argb & 255) / 255.0F, ((argb >>> 24) & 255) / 255.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(x1, y2, 0.0F);
        GL11.glVertex3f(x2, y2, 0.0F);
        GL11.glVertex3f(x2, y1, 0.0F);
        GL11.glVertex3f(x1, y1, 0.0F);
        GL11.glEnd();
    }

    static void outline(Box a, int r, int g, int b, int al) {
        GL11.glColor4f(r / 255.0F, g / 255.0F, b / 255.0F, al / 255.0F);
        GL11.glBegin(GL11.GL_LINES);
        double[] xs = {a.minX, a.maxX}, ys = {a.minY, a.maxY}, zs = {a.minZ, a.maxZ};
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GL11.glVertex3d(xs[0], ys[i], zs[j]);
                GL11.glVertex3d(xs[1], ys[i], zs[j]);
                GL11.glVertex3d(xs[i], ys[0], zs[j]);
                GL11.glVertex3d(xs[i], ys[1], zs[j]);
                GL11.glVertex3d(xs[i], ys[j], zs[0]);
                GL11.glVertex3d(xs[i], ys[j], zs[1]);
            }
        }
        GL11.glEnd();
    }

    static void filled(Box a, int r, int g, int b, int al) {
        GL11.glColor4f(r / 255.0F, g / 255.0F, b / 255.0F, al / 255.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(a.minX, a.minY, a.minZ); GL11.glVertex3d(a.maxX, a.minY, a.minZ);
        GL11.glVertex3d(a.maxX, a.minY, a.maxZ); GL11.glVertex3d(a.minX, a.minY, a.maxZ);
        GL11.glVertex3d(a.minX, a.maxY, a.minZ); GL11.glVertex3d(a.minX, a.maxY, a.maxZ);
        GL11.glVertex3d(a.maxX, a.maxY, a.maxZ); GL11.glVertex3d(a.maxX, a.maxY, a.minZ);
        GL11.glVertex3d(a.minX, a.minY, a.minZ); GL11.glVertex3d(a.minX, a.maxY, a.minZ);
        GL11.glVertex3d(a.maxX, a.maxY, a.minZ); GL11.glVertex3d(a.maxX, a.minY, a.minZ);
        GL11.glVertex3d(a.minX, a.minY, a.maxZ); GL11.glVertex3d(a.maxX, a.minY, a.maxZ);
        GL11.glVertex3d(a.maxX, a.maxY, a.maxZ); GL11.glVertex3d(a.minX, a.maxY, a.maxZ);
        GL11.glVertex3d(a.minX, a.minY, a.minZ); GL11.glVertex3d(a.minX, a.minY, a.maxZ);
        GL11.glVertex3d(a.minX, a.maxY, a.maxZ); GL11.glVertex3d(a.minX, a.maxY, a.minZ);
        GL11.glVertex3d(a.maxX, a.minY, a.minZ); GL11.glVertex3d(a.maxX, a.maxY, a.minZ);
        GL11.glVertex3d(a.maxX, a.maxY, a.maxZ); GL11.glVertex3d(a.maxX, a.minY, a.maxZ);
        GL11.glEnd();
    }
}
