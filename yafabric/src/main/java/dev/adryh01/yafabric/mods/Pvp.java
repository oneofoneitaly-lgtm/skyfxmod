package dev.adryh01.yafabric.mods;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** 1.7 animations, Hitbox and Block outline (the world-drawing PvP modules). */
public final class Pvp {

    static final OldAnimations ANIM = new OldAnimations();
    static final Hitbox HITBOX = new Hitbox();
    static final BlockOutline OUTLINE = new BlockOutline();

    private Pvp() {
    }

    static void register(List<Module> all) {
        all.add(ANIM);
        all.add(HITBOX);
        all.add(OUTLINE);
    }

    // ------------------------------------------------------------ hooks called from the mixins

    private static boolean handPushed;

    /** Start of the first-person hand drawing (HeldItemRenderer). */
    public static void handBegin() {
        handPushed = false;
        if (!ANIM.enabled) return;
        try {
            ANIM.poll();
            handPushed = ANIM.transform();
        } catch (RuntimeException e) {
            handPushed = false;
        }
    }

    public static void handEnd() {
        if (handPushed) {
            GL11.glPopMatrix();
            handPushed = false;
        }
    }

    /** After all entities were drawn: camera space, ready for lines. */
    public static void world(float pt) {
        if (HITBOX.enabled) {
            try {
                HITBOX.render(pt);
            } catch (RuntimeException e) {
                System.out.println("[YA CLIENT] hitbox error: " + e);
            }
        }
    }

    /** Returns true when we drew the block outline ourselves. */
    public static boolean outline(PlayerEntity pl, BlockHitResult hit, float pt) {
        if (!OUTLINE.enabled) return false;
        try {
            return OUTLINE.draw(pl, hit, pt);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------ 1.7 animations

    /** Swing animation while blocking / eating / drawing a bow, like Minecraft 1.7. */
    static final class OldAnimations extends Module {
        final Setting blockHit = Setting.bool("blockhit", "Block-hit swing (1.7)", true);
        final Setting useSwing = Setting.bool("useswing", "Swing while eating / bow", true);
        final Setting power = Setting.slider("power", "Swing strength", 1.0F, 0.5F, 1.5F, 0.1F);

        private boolean wasDown;
        private long lastSwing;
        private static final long SWING_MS = 300L;

        OldAnimations() {
            super("oldanimations", "1.7 Animations", "Swing while blocking, eating or drawing a bow, like 1.7.", "PvP");
            settings.add(blockHit);
            settings.add(useSwing);
            settings.add(power);
        }

        private boolean active() {
            ClientPlayerEntity p = mc.player;
            if (p == null || !p.isUsingItem()) return false;
            ItemStack h = p.getStackInHand();
            boolean blocking = h != null && h.getItem().getUseAction(h) == UseAction.BLOCK;
            return blocking ? blockHit.b : useSwing.b;
        }

        /** Attack button edge, checked every tick and every frame. */
        void poll() {
            boolean down = Mouse.isButtonDown(0);
            if (down && !wasDown && mc.currentScreen == null) leftClick();
            wasDown = down;
        }

        @Override
        public void onTick() {
            poll();
        }

        /**
         * Vanilla 1.8.9 throws away attack clicks while you block / eat / draw a bow. Like in 1.7 we let the
         * click through: the arm swings and, if you aim at something, the hit is sent.
         */
        private void leftClick() {
            long now = System.currentTimeMillis();
            if (now - lastSwing < 120L) return;
            ClientPlayerEntity p = mc.player;
            if (p == null || mc.currentScreen != null || !active()) return;
            lastSwing = now;
            p.swingHand();
            BlockHitResult o = mc.result;
            if (o != null && o.entity != null && mc.interactionManager != null) {
                mc.interactionManager.attackEntity(p, o.entity);
            }
        }

        /** Applies the swing to the hand matrix. Returns true when a matrix was pushed (the caller pops it). */
        boolean transform() {
            if (!active()) return false;
            float sp = (System.currentTimeMillis() - lastSwing) / (float) SWING_MS;
            if (lastSwing == 0L || sp <= 0.0F || sp >= 1.0F) return false;
            float k = power.v;
            float f = (float) Math.sin(sp * sp * Math.PI);
            float f1 = (float) Math.sin(Math.sqrt(sp) * Math.PI);
            GL11.glPushMatrix();
            GL11.glTranslatef(-f1 * 0.4F * k, (float) Math.sin(Math.sqrt(sp) * Math.PI * 2.0D) * 0.2F * k,
                    (float) -Math.sin(sp * Math.PI) * 0.2F * k);
            GL11.glTranslatef(0.56F, -0.52F, -0.72F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(f * -20.0F * k, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(f1 * -20.0F * k, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(f1 * -80.0F * k, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(-0.56F, 0.52F, 0.72F);
            return true;
        }
    }

    // ------------------------------------------------------------ Hitbox

    static final class Hitbox extends Module {
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

        void render(float pt) {
            if (mc.world == null || mc.player == null) return;
            EntityRenderDispatcher rm = mc.getEntityRenderManager();
            Entity aimed = mc.result != null ? mc.result.entity : null;
            int st = Gl.save();
            Gl.flat();
            if (walls.b) GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glLineWidth(width.v);
            int alpha = (int) (opacity.v * 2.55F);
            List<Entity> list = mc.world.loadedEntities;
            for (int i = 0; i < list.size(); i++) {
                Entity e = list.get(i);
                if (e == null) continue;
                if (e == mc.player && mc.options.perspective == 0) continue;
                if (who.idx == 0 && !(e instanceof PlayerEntity)) continue;
                if (who.idx == 1 && !(e instanceof LivingEntity)) continue;
                if (e.isInvisibleTo(mc.player) && !(e instanceof PlayerEntity)) continue;
                double ix = e.prevTickX + (e.x - e.prevTickX) * pt;
                double iy = e.prevTickY + (e.y - e.prevTickY) * pt;
                double iz = e.prevTickZ + (e.z - e.prevTickZ) * pt;
                Box bb = e.getBoundingBox();
                if (real.b) bb = bb.expand(0.1D, 0.1D, 0.1D);
                Box box = bb.offset(ix - e.x - rm.cameraX, iy - e.y - rm.cameraY, iz - e.z - rm.cameraZ);
                int rgb = e == aimed ? target.rgb() : color.rgb();
                Gl.outline(box, Gl.r(rgb), Gl.g(rgb), Gl.b(rgb), alpha);
            }
            Gl.restore(st);
        }
    }

    // ------------------------------------------------------------ Block outline

    static final class BlockOutline extends Module {
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

        boolean draw(PlayerEntity pl, BlockHitResult t, float pt) {
            if (t == null || t.type != BlockHitResult.Type.BLOCK || mc.world == null) return false;
            World w = mc.world;
            BlockPos pos = t.getBlockPos();
            Block block = w.getBlockState(pos).getBlock();
            if (block.getMaterial() == Material.AIR || !w.getWorldBorder().contains(pos)) return false;
            block.setBoundingBox(w, pos);
            double dx = pl.prevTickX + (pl.x - pl.prevTickX) * pt;
            double dy = pl.prevTickY + (pl.y - pl.prevTickY) * pt;
            double dz = pl.prevTickZ + (pl.z - pl.prevTickZ) * pt;
            Box bb = block.getSelectionBox(w, pos).expand(0.002D, 0.002D, 0.002D).offset(-dx, -dy, -dz);
            int st = Gl.save();
            Gl.flat();
            GL11.glDepthMask(false);
            if (fill.b) {
                int f = fillColor.rgb();
                Gl.filled(bb, Gl.r(f), Gl.g(f), Gl.b(f), (int) (fillOpacity.v * 2.55F));
            }
            GL11.glLineWidth(width.v);
            int c = color.rgb();
            Gl.outline(bb, Gl.r(c), Gl.g(c), Gl.b(c), (int) (opacity.v * 2.55F));
            Gl.restore(st);
            return true; // we drew it ourselves: hide the vanilla black outline
        }
    }
}
