package dev.adryh01.yamods;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.lwjgl.opengl.GL11;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** PvP and visual modules that draw in the 3D world or change the camera / hand. */
final class Pvp {

    private Pvp() {
    }

    private static int r(int rgb) {
        return (rgb >> 16) & 255;
    }

    private static int g(int rgb) {
        return (rgb >> 8) & 255;
    }

    private static int b(int rgb) {
        return rgb & 255;
    }

    // ------------------------------------------------------------ Smooth Shift

    /** The camera glides down and up when you sneak, instead of jumping. */
    static class SmoothSneak extends Module {
        final Setting speed = Setting.slider("speed", "Speed", 9.0F, 3.0F, 20.0F, 1.0F);
        private float s;
        private long last;
        private boolean init;

        SmoothSneak() {
            super("smoothsneak", "Smooth Shift", "The camera glides smoothly when you sneak and un-sneak.", "PvP");
            settings.add(speed);
        }

        @Override
        public void camera(EntityViewRenderEvent.CameraSetup e) {
            if (e.entity != mc.thePlayer) return;
            long now = System.nanoTime();
            float dt = init ? Math.min(0.1F, (now - last) / 1.0E9F) : 0.0F;
            last = now;
            float v = mc.thePlayer.isSneaking() ? -0.08F : 0.0F;
            if (!init) {
                s = v;
                init = true;
            }
            s += (v - s) * (1.0F - (float) Math.exp(-dt * speed.v));
            float extra = s - v; // how much higher the camera still is than the vanilla one
            if (Math.abs(extra) < 0.0004F) return;
            // move the world down by 'extra', expressed in view space (before the camera rotations are applied)
            double p = Math.toRadians(e.pitch), ro = Math.toRadians(e.roll);
            double cp = Math.cos(p);
            GlStateManager.translate((float) (extra * cp * Math.sin(ro)), (float) (-extra * cp * Math.cos(ro)), (float) (-extra * Math.sin(p)));
        }

        @Override
        public void onDisable() {
            init = false;
        }
    }

    // ------------------------------------------------------------ 1.7 animations

    /** Swing animation while blocking / eating / drawing a bow, like Minecraft 1.7. */
    static class OldAnimations extends Module {
        final Setting blockHit = Setting.bool("blockhit", "Block-hit swing (1.7)", true);
        final Setting useSwing = Setting.bool("useswing", "Swing while eating / bow", true);
        final Setting power = Setting.slider("power", "Swing strength", 1.0F, 0.5F, 1.5F, 0.1F);

        OldAnimations() {
            super("oldanimations", "1.7 Animations", "Swing while blocking, eating or drawing a bow, like 1.7.", "PvP");
            settings.add(blockHit);
            settings.add(useSwing);
            settings.add(power);
        }

        /**
         * Vanilla 1.8.9 throws away attack clicks while you block / eat / draw a bow. Like in 1.7 we let the
         * click through: the arm swings and, if you aim at something, the hit is sent.
         */
        private boolean wasDown;
        private long lastSwing;
        private static final long SWING_MS = 300L;

        private boolean active() {
            EntityPlayerSP p = mc.thePlayer;
            if (p == null || !p.isUsingItem()) return false;
            ItemStack h = p.getHeldItem();
            boolean blocking = h != null && h.getItem().getItemUseAction(h) == EnumAction.BLOCK;
            return blocking ? blockHit.b : useSwing.b;
        }

        /** Attack button edge, checked from every possible place (mouse event, tick and every frame). */
        private void poll() {
            boolean down = org.lwjgl.input.Mouse.isButtonDown(0);
            if (down && !wasDown && mc.currentScreen == null) leftClick();
            wasDown = down;
        }

        @Override
        public void onTick() {
            poll();
        }

        @Override
        public void leftClick() {
            long now = System.currentTimeMillis();
            if (now - lastSwing < 120L) return; // the same click seen twice
            EntityPlayerSP p = mc.thePlayer;
            if (p == null || mc.currentScreen != null || !active()) return;
            lastSwing = now;
            p.swingItem();
            MovingObjectPosition o = mc.objectMouseOver;
            if (o != null && o.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && o.entityHit != null) {
                mc.playerController.attackEntity(p, o.entityHit);
            }
        }

        @Override
        public void renderHand(float pt) {
            poll();
            if (!active()) return;
            // our own swing clock: the vanilla swing progress is ignored by the item renderer while using an item
            float sp = (System.currentTimeMillis() - lastSwing) / (float) SWING_MS;
            if (lastSwing == 0L || sp <= 0.0F || sp >= 1.0F) return;
            float k = power.v;
            float f = (float) Math.sin(sp * sp * Math.PI);
            float f1 = (float) Math.sin(Math.sqrt(sp) * Math.PI);
            GlStateManager.translate(-f1 * 0.4F * k, (float) Math.sin(Math.sqrt(sp) * Math.PI * 2.0D) * 0.2F * k,
                    (float) -Math.sin(sp * Math.PI) * 0.2F * k);
            GlStateManager.translate(0.56F, -0.52F, -0.72F);
            GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f * -20.0F * k, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f1 * -20.0F * k, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f1 * -80.0F * k, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-45.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(-0.56F, 0.52F, 0.72F);
        }
    }

    // ------------------------------------------------------------ Hit Hearts

    static class HitHearts extends Module {
        static final class P {
            double x, y, z;
            long born;
            EntityLivingBase e;
            boolean label;
        }

        private static final Gui GUI = new Gui();
        final Setting count = Setting.slider("count", "Hearts per hit", 4.0F, 1.0F, 10.0F, 1.0F);
        final Setting life = Setting.slider("life", "Duration (s)", 1.2F, 0.5F, 3.0F, 0.1F);
        final Setting size = Setting.slider("size", "Size", 1.0F, 0.5F, 2.0F, 0.1F);
        final Setting color = Setting.color("color", "Heart colour", 1);
        final Setting health = Setting.bool("health", "Show target health", true);
        private final List<P> list = new ArrayList<P>();
        private final Random rnd = new Random();

        HitHearts() {
            super("hithearts", "Hit Hearts", "Hearts and the target's health pop up on the player you hit.", "PvP");
            settings.add(count);
            settings.add(life);
            settings.add(size);
            settings.add(color);
            settings.add(health);
        }

        @Override
        public void attacked(Entity t) {
            if (!(t instanceof EntityLivingBase)) return;
            EntityLivingBase e = (EntityLivingBase) t;
            long now = System.currentTimeMillis();
            for (int i = 0; i < (int) count.v; i++) {
                P p = new P();
                // from the middle of the hitbox upwards
                p.x = e.posX + (rnd.nextDouble() - 0.5D) * e.width;
                p.y = e.posY + e.height * 0.5D + rnd.nextDouble() * e.height * 0.6D;
                p.z = e.posZ + (rnd.nextDouble() - 0.5D) * e.width;
                p.born = now - rnd.nextInt(120);
                p.e = e;
                list.add(p);
            }
            if (health.b) {
                P p = new P();
                p.x = e.posX;
                p.y = e.posY + e.height + 0.35D;
                p.z = e.posZ;
                p.born = now;
                p.e = e;
                p.label = true;
                list.add(p);
            }
            if (list.size() > 120) list.subList(0, list.size() - 120).clear();
        }

        private static final String[] SHAPE = {
                ".XX...XX.",
                "XXXX.XXXX",
                "XXXXXXXXX",
                "XXXXXXXXX",
                ".XXXXXXX.",
                "..XXXXX..",
                "...XXX...",
                "....X...."};

        private static boolean cell(int r, int c) {
            return r >= 0 && r < SHAPE.length && c >= 0 && c < 9 && SHAPE[r].charAt(c) == 'X';
        }

        /** A pixel heart in any colour (fill + darker outline + a small highlight), drawn in one batch. */
        private static void heart(int x, int y, int rgb, float alpha) {
            int a = (int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
            int fr = (rgb >> 16) & 255, fg = (rgb >> 8) & 255, fb = rgb & 255;
            int or = fr * 35 / 100, og = fg * 35 / 100, ob = fb * 35 / 100;
            GlStateManager.disableTexture2D();
            GlStateManager.disableCull();
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            w.begin(7, DefaultVertexFormats.POSITION_COLOR);
            for (int r = -1; r <= SHAPE.length; r++) {
                for (int c = -1; c <= 9; c++) {
                    int cr, cg, cb;
                    if (cell(r, c)) {
                        boolean hi = r == 1 && c == 1;
                        cr = hi ? Math.min(255, fr + 110) : fr;
                        cg = hi ? Math.min(255, fg + 110) : fg;
                        cb = hi ? Math.min(255, fb + 110) : fb;
                    } else if (cell(r - 1, c) || cell(r + 1, c) || cell(r, c - 1) || cell(r, c + 1)) {
                        cr = or;
                        cg = og;
                        cb = ob;
                    } else {
                        continue;
                    }
                    double x0 = x + c, y0 = y + r;
                    w.pos(x0, y0 + 1, 0).color(cr, cg, cb, a).endVertex();
                    w.pos(x0 + 1, y0 + 1, 0).color(cr, cg, cb, a).endVertex();
                    w.pos(x0 + 1, y0, 0).color(cr, cg, cb, a).endVertex();
                    w.pos(x0, y0, 0).color(cr, cg, cb, a).endVertex();
                }
            }
            t.draw();
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
        }

        @Override
        public void renderWorld(float pt) {
            if (list.isEmpty()) return;
            RenderManager rm = mc.getRenderManager();
            FontRenderer fr = mc.fontRendererObj;
            long now = System.currentTimeMillis();
            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            for (Iterator<P> it = list.iterator(); it.hasNext(); ) {
                P p = it.next();
                float age = (now - p.born) / (life.v * 1000.0F);
                if (age >= 1.0F || p.e == null || p.e.isDead) {
                    it.remove();
                    continue;
                }
                if (age < 0.0F) age = 0.0F;
                float a = age < 0.6F ? 1.0F : (1.0F - age) / 0.4F;
                GlStateManager.pushMatrix();
                GlStateManager.translate(p.x - rm.viewerPosX, p.y + age * 0.7D - rm.viewerPosY, p.z - rm.viewerPosZ);
                GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                float s = 0.025F * size.v;
                GlStateManager.scale(-s, -s, s);
                int rgb = color.rgb();
                if (p.label) {
                    String t = Q.f("%.1f", p.e.getHealth());
                    int w = 11 + fr.getStringWidth(t);
                    heart(-w / 2, -5, rgb, a);
                    int al = (int) (a * 255.0F);
                    if (al > 8) fr.drawStringWithShadow(t, -w / 2 + 11, -4, (al << 24) | 0xFFFFFF);
                } else {
                    heart(-5, -5, rgb, a);
                }
                GlStateManager.popMatrix();
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    // ------------------------------------------------------------ Hitbox

    static class Hitbox extends Module {
        final Setting who = Setting.choice("who", "Show on", 0, "Players", "All living", "Everything");
        final Setting color = Setting.color("color", "Colour", 0);
        final Setting target = Setting.color("target", "Colour when aiming", 1);
        final Setting width = Setting.slider("width", "Line width", 2.0F, 1.0F, 6.0F, 1.0F);
        final Setting opacity = Setting.slider("opacity", "Opacity (%)", 100.0F, 20.0F, 100.0F, 5.0F);
        final Setting real = Setting.bool("real", "Real hit area", false);
        final Setting walls = Setting.bool("walls", "Through walls", true);

        Hitbox() {
            super("hitbox", "Hitbox", "Coloured hitbox around players (colour changes when you aim at one).", "PvP");
            settings.add(who);
            settings.add(color);
            settings.add(target);
            settings.add(width);
            settings.add(opacity);
            settings.add(real);
            settings.add(walls);
        }

        @Override
        public void renderWorld(float pt) {
            if (mc.theWorld == null) return;
            RenderManager rm = mc.getRenderManager();
            Entity aimed = mc.objectMouseOver != null ? mc.objectMouseOver.entityHit : null;
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            if (walls.b) GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GL11.glLineWidth(width.v);
            int alpha = (int) (opacity.v * 2.55F);
            for (Object o : mc.theWorld.loadedEntityList) {
                Entity e = (Entity) o;
                if (e == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;
                if (who.idx == 0 && !(e instanceof EntityPlayer)) continue;
                if (who.idx == 1 && !(e instanceof EntityLivingBase)) continue;
                if (e.isInvisibleToPlayer(mc.thePlayer) && !(e instanceof EntityPlayer)) continue;
                double ix = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt;
                double iy = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt;
                double iz = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt;
                AxisAlignedBB bb = e.getEntityBoundingBox();
                if (real.b) {
                    float c = e.getCollisionBorderSize();
                    bb = bb.expand(c, c, c);
                }
                AxisAlignedBB box = bb.offset(ix - e.posX - rm.viewerPosX, iy - e.posY - rm.viewerPosY, iz - e.posZ - rm.viewerPosZ);
                int rgb = e == aimed ? target.rgb() : color.rgb();
                RenderGlobal.drawOutlinedBoundingBox(box, r(rgb), g(rgb), b(rgb), alpha);
            }
            GL11.glLineWidth(1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    // ------------------------------------------------------------ Block outline

    static class BlockOutline extends Module {
        final Setting color = Setting.color("color", "Outline colour", 0);
        final Setting width = Setting.slider("width", "Thickness", 2.0F, 1.0F, 6.0F, 1.0F);
        final Setting opacity = Setting.slider("opacity", "Outline opacity (%)", 100.0F, 10.0F, 100.0F, 5.0F);
        final Setting fill = Setting.bool("fill", "Fill the block", true);
        final Setting fillColor = Setting.color("fillcolor", "Fill colour", 0);
        final Setting fillOpacity = Setting.slider("fillopacity", "Fill opacity (%)", 25.0F, 5.0F, 80.0F, 5.0F);

        BlockOutline() {
            super("blockoutline", "Block Outline", "Colour, thickness and fill of the block you look at.", "Visual");
            settings.add(color);
            settings.add(width);
            settings.add(opacity);
            settings.add(fill);
            settings.add(fillColor);
            settings.add(fillOpacity);
        }

        @Override
        public boolean drawBlock(DrawBlockHighlightEvent e) {
            MovingObjectPosition t = e.target;
            if (t == null || t.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
            World w = mc.theWorld;
            BlockPos pos = t.getBlockPos();
            Block block = w.getBlockState(pos).getBlock();
            if (block.getMaterial() == Material.air || !w.getWorldBorder().contains(pos)) return false;
            block.setBlockBoundsBasedOnState(w, pos);
            EntityPlayer pl = e.player;
            double dx = pl.lastTickPosX + (pl.posX - pl.lastTickPosX) * e.partialTicks;
            double dy = pl.lastTickPosY + (pl.posY - pl.lastTickPosY) * e.partialTicks;
            double dz = pl.lastTickPosZ + (pl.posZ - pl.lastTickPosZ) * e.partialTicks;
            AxisAlignedBB bb = block.getSelectedBoundingBox(w, pos).expand(0.002D, 0.002D, 0.002D).offset(-dx, -dy, -dz);

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            if (fill.b) {
                int f = fillColor.rgb();
                drawFilled(bb, r(f), g(f), b(f), (int) (fillOpacity.v * 2.55F));
            }
            GL11.glLineWidth(width.v);
            int c = color.rgb();
            RenderGlobal.drawOutlinedBoundingBox(bb, r(c), g(c), b(c), (int) (opacity.v * 2.55F));
            GL11.glLineWidth(1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            return true; // we drew it ourselves: hide the vanilla black outline
        }

        private static void drawFilled(AxisAlignedBB a, int r, int g, int b, int al) {
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            w.begin(7, DefaultVertexFormats.POSITION_COLOR);
            w.pos(a.minX, a.minY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.minY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.minY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.minY, a.maxZ).color(r, g, b, al).endVertex();

            w.pos(a.minX, a.maxY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.maxY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.maxY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.maxY, a.minZ).color(r, g, b, al).endVertex();

            w.pos(a.minX, a.minY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.maxY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.maxY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.minY, a.minZ).color(r, g, b, al).endVertex();

            w.pos(a.minX, a.minY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.minY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.maxY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.maxY, a.maxZ).color(r, g, b, al).endVertex();

            w.pos(a.minX, a.minY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.minY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.maxY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.minX, a.maxY, a.minZ).color(r, g, b, al).endVertex();

            w.pos(a.maxX, a.minY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.maxY, a.minZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.maxY, a.maxZ).color(r, g, b, al).endVertex();
            w.pos(a.maxX, a.minY, a.maxZ).color(r, g, b, al).endVertex();
            t.draw();
        }
    }

    // ------------------------------------------------------------ small extras

    static class TimeChanger extends Module {
        private static final long[] TICKS = {1000L, 6000L, 12500L, 14000L, 18000L};
        final Setting time = Setting.choice("time", "Time of day", 0, "Morning", "Noon", "Sunset", "Night", "Midnight");

        TimeChanger() {
            super("timechanger", "Time Changer", "Pick the time of day (only on your screen).", "Visual");
            settings.add(time);
        }

        @Override
        public void onTick() {
            if (mc.theWorld != null) mc.theWorld.setWorldTime(TICKS[time.idx]);
        }
    }

    static class ClearWeather extends Module {
        ClearWeather() {
            super("clearweather", "Clear Weather", "Removes rain and thunder from your screen.", "Visual");
        }

        @Override
        public void onTick() {
            World w = mc.theWorld;
            if (w == null) return;
            w.prevRainingStrength = 0.0F;
            w.rainingStrength = 0.0F;
            w.prevThunderingStrength = 0.0F;
            w.thunderingStrength = 0.0F;
        }
    }

    static class NoHurtCam extends Module {
        NoHurtCam() {
            super("nohurtcam", "No Hurt Cam", "The screen does not shake when you take damage.", "PvP");
        }

        @Override
        public void onTick() {
            mc.thePlayer.hurtTime = 0;
        }
    }

    static class ChatTime extends Module {
        final Setting h24 = Setting.bool("h24", "24-hour clock", true);
        final Setting sec = Setting.bool("sec", "Show seconds", false);

        ChatTime() {
            super("chattime", "Chat Timestamps", "Adds the time in front of every chat message.", "Utility");
            settings.add(h24);
            settings.add(sec);
        }

        @Override
        public void chat(ClientChatReceivedEvent e) {
            if (e.type == 2 || e.message == null) return; // 2 = action bar
            String f = h24.b ? (sec.b ? "HH:mm:ss" : "HH:mm") : (sec.b ? "hh:mm:ss a" : "hh:mm a");
            IChatComponent prefix = new ChatComponentText("[" + new SimpleDateFormat(f, Locale.ROOT).format(new Date()) + "] ");
            prefix.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));
            prefix.appendSibling(e.message);
            e.message = prefix;
        }
    }
}
