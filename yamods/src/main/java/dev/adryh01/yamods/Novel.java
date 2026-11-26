package dev.adryh01.yamods;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Newer PvP ideas: hit window, throw preview, fight recap, hit coach, sounds, kill effects, damage popups, target gear. */
final class Novel {

    private Novel() {
    }

    // ------------------------------------------------------------ Hit Window

    /** Shows when the player you aim at can take damage again (the 10-tick invulnerability window). */
    static class HitWindow extends Module {
        private EntityLivingBase target;
        private long seen;

        HitWindow() {
            super("hitwindow", "Hit Window", "Shows when your target can be hit again, so you time your hits.", "PvP");
            asHud(0.5F, 0.56F, 0);
            moved = true;
        }

        @Override
        public void onTick() {
            MovingObjectPosition o = mc.objectMouseOver;
            if (o != null && o.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && o.entityHit instanceof EntityLivingBase) {
                target = (EntityLivingBase) o.entityHit;
                seen = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - seen > 1500L || (target != null && target.isDead)) {
                target = null;
            }
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final EntityLivingBase e = target;
            if (e == null && !editing) return;
            final int hrt = e == null ? 14 : e.hurtResistantTime;
            final int max = e == null ? 20 : Math.max(1, e.maxHurtResistantTime);
            final boolean ready = hrt <= max / 2;
            final float f = Math.max(0.0F, Math.min(1.0F, hrt / (float) max));
            Hud.box(this, sw, sh, 70, 20, new Hud.Painter() {
                @Override
                public void paint(Module m, FontRenderer fr) {
                    String t = ready ? "HIT NOW" : "WAIT";
                    m.override = ready ? 0x55FF55 : 0xFF5555;
                    Hud.text(m, fr, t, 35 - fr.getStringWidth(t) / 2, 3);
                    m.override = 0;
                    Gui.drawRect(3, 13, 67, 17, 0x80000000);
                    Gui.drawRect(3, 13, 3 + (int) (64 * f), 17, ready ? 0xFF55FF55 : 0xFFFF5555);
                }
            });
        }
    }

    // ------------------------------------------------------------ Throw Preview

    /** Draws the path and landing spot of ender pearls, splash potions, snowballs, eggs and bow shots. */
    static class ThrowPreview extends Module {
        final Setting color = Setting.color("color", "Line colour", 5);
        final Setting width = Setting.slider("width", "Line width", 2.0F, 1.0F, 4.0F, 1.0F);
        final Setting marker = Setting.bool("marker", "Landing marker", true);
        final Setting bow = Setting.bool("bow", "Include bow shots", true);

        ThrowPreview() {
            super("throwpreview", "Throw Preview", "Shows where pearls, potions and arrows will land.", "PvP");
            settings.add(color);
            settings.add(width);
            settings.add(marker);
            settings.add(bow);
        }

        @Override
        public void renderWorld(float pt) {
            EntityPlayerSP p = mc.thePlayer;
            ItemStack h = p.getHeldItem();
            if (h == null || mc.theWorld == null) return;
            Item it = h.getItem();
            double speed, grav, offset = 0.0D;
            if (it == Items.ender_pearl || it == Items.snowball || it == Items.egg) {
                speed = 1.5D;
                grav = 0.03D;
            } else if (it instanceof ItemPotion && ItemPotion.isSplash(h.getMetadata())) {
                speed = 0.5D;
                grav = 0.05D;
                offset = -20.0D;
            } else if (bow.b && it instanceof ItemBow && p.isUsingItem()) {
                float f = p.getItemInUseDuration() / 20.0F;
                f = (f * f + f * 2.0F) / 3.0F;
                if (f < 0.1F) return;
                if (f > 1.0F) f = 1.0F;
                speed = f * 3.0D;
                grav = 0.05D;
            } else {
                return;
            }

            float yaw = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * pt;
            float pitch = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * pt;
            double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt + p.getEyeHeight();
            double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
            double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
            x -= Math.cos(yr) * 0.16D;
            y -= 0.1D;
            z -= Math.sin(yr) * 0.16D;
            double mx = -Math.sin(yr) * Math.cos(pr);
            double my = -Math.sin(Math.toRadians(pitch + offset));
            double mz = Math.cos(yr) * Math.cos(pr);
            double len = Math.sqrt(mx * mx + my * my + mz * mz);
            mx = mx / len * speed;
            my = my / len * speed;
            mz = mz / len * speed;

            List<double[]> pts = new ArrayList<double[]>();
            pts.add(new double[]{x, y, z});
            double[] land = null;
            for (int i = 0; i < 160; i++) {
                Vec3 a = new Vec3(x, y, z);
                Vec3 b = new Vec3(x + mx, y + my, z + mz);
                MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(a, b, false, true, false);
                if (hit != null && hit.hitVec != null) {
                    land = new double[]{hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord};
                    pts.add(land);
                    break;
                }
                x += mx;
                y += my;
                z += mz;
                pts.add(new double[]{x, y, z});
                mx *= 0.99D;
                my = my * 0.99D - grav;
                mz *= 0.99D;
                if (y < 0.0D) break;
            }

            RenderManager rm = mc.getRenderManager();
            int rgb = color.rgb();
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.depthMask(false);
            GL11.glLineWidth(width.v);
            GlStateManager.color(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, 0.9F);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (double[] q : pts) GL11.glVertex3d(q[0] - rm.viewerPosX, q[1] - rm.viewerPosY, q[2] - rm.viewerPosZ);
            GL11.glEnd();
            if (marker.b && land != null) {
                AxisAlignedBB bb = new AxisAlignedBB(land[0] - 0.2D - rm.viewerPosX, land[1] - 0.2D - rm.viewerPosY, land[2] - 0.2D - rm.viewerPosZ,
                        land[0] + 0.2D - rm.viewerPosX, land[1] + 0.2D - rm.viewerPosY, land[2] + 0.2D - rm.viewerPosZ);
                RenderGlobal.drawOutlinedBoundingBox(bb, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
            }
            GL11.glLineWidth(1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    // ------------------------------------------------------------ Fight Recap

    /** A small card with the stats of your last fight. */
    static class FightRecap extends Module {
        final Setting show = Setting.slider("show", "Show for (s)", 20.0F, 5.0F, 60.0F, 5.0F);

        FightRecap() {
            super("fightrecap", "Fight Recap", "Stats of your last fight: hits, accuracy, damage taken, best combo.", "PvP");
            settings.add(show);
            asHud(0.01F, 0.3F, 0);
            background.b = true;
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            List<String> l = new ArrayList<String>();
            if (Q.fightActive) {
                l.add("Fighting...");
                l.add("Hits: " + Q.fightHits + " / " + Math.max(Q.fightSwings, Q.fightHits) + " swings");
                l.add("Combo: " + Q.combo);
            } else if (Q.recap != null && System.currentTimeMillis() - Q.recapTime < show.v * 1000.0F) {
                l.addAll(Q.recap);
            } else if (editing) {
                l.add("Fight recap: Win");
                l.add("Duration: 12.4 s");
                l.add("Hits: 18 / 27 swings (66%)");
                l.add("Damage taken: 7.5 HP");
                l.add("Best combo: 5");
            }
            Hud.lines(this, sw, sh, l);
        }
    }

    // ------------------------------------------------------------ Hit Coach

    /** Live PvP stats: how often you hit while sprinting, crit and connect. */
    static class HitCoach extends Module {
        HitCoach() {
            super("hitcoach", "Hit Coach", "Hit rate, sprint hits and crits, to see how you really fight.", "PvP");
            asHud(0.01F, 0.5F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            int hits = Math.max(1, Q.hits);
            int rate = Q.swings <= 0 ? 0 : Math.min(100, Q.hits * 100 / Q.swings);
            List<String> l = new ArrayList<String>();
            l.add("Hit rate: " + rate + "%");
            l.add("Sprint hits: " + (Q.sprintHits * 100 / hits) + "%");
            l.add("Crits: " + (Q.critHits * 100 / hits) + "%");
            l.add("Best CPS: " + Q.maxCps);
            Hud.lines(this, sw, sh, l);
        }
    }

    // ------------------------------------------------------------ Hit Sounds

    static class HitSounds extends Module {
        private static final String[] SOUNDS = {"random.successful_hit", "note.pling", "random.orb", "random.anvil_land",
                "mob.blaze.hit", "random.pop", "note.bell"};
        final Setting sound = Setting.choice("sound", "Sound", 0, "Ding", "Pling", "Orb", "Anvil", "Blaze", "Pop", "Bell");
        final Setting volume = Setting.slider("volume", "Volume", 0.8F, 0.1F, 1.0F, 0.1F);
        final Setting pitch = Setting.slider("pitch", "Pitch", 1.0F, 0.5F, 2.0F, 0.1F);
        final Setting combo = Setting.bool("combo", "Pitch rises with your combo", true);

        HitSounds() {
            super("hitsounds", "Hit Sounds", "A sound every time you hit someone. The pitch can climb with your combo.", "PvP");
            settings.add(sound);
            settings.add(volume);
            settings.add(pitch);
            settings.add(combo);
        }

        @Override
        public void attacked(Entity t) {
            if (!(t instanceof EntityLivingBase) || mc.thePlayer == null) return;
            float p = pitch.v;
            if (combo.b) p += Math.min(10, Q.combo) * 0.06F;
            p = Math.max(0.5F, Math.min(2.0F, p));
            mc.thePlayer.playSound(SOUNDS[sound.idx], volume.v, p);
        }
    }

    // ------------------------------------------------------------ Kill Effects

    static class KillEffects extends Module {
        final Setting effect = Setting.choice("effect", "Effect", 0, "Lightning", "Explosion", "Fireworks", "Hearts", "Blood");
        final Setting sound = Setting.bool("sound", "Level-up sound", false);
        private int seen = -1;
        private final Random rnd = new Random();

        KillEffects() {
            super("killeffects", "Kill Effects", "A visual effect (only for you) when you kill a player or mob.", "PvP");
            settings.add(effect);
            settings.add(sound);
        }

        @Override
        public void onEnable() {
            seen = Q.kills;
        }

        @Override
        public void onTick() {
            if (seen < 0) seen = Q.kills;
            if (Q.kills == seen) return;
            seen = Q.kills;
            double x = Q.killX, y = Q.killY, z = Q.killZ;
            switch (effect.idx) {
                case 0:
                    mc.theWorld.addWeatherEffect(new EntityLightningBolt(mc.theWorld, x, y, z));
                    break;
                case 1:
                    mc.theWorld.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, x, y + 1.0D, z, 0.0D, 0.0D, 0.0D);
                    break;
                case 2:
                    for (int i = 0; i < 40; i++) {
                        mc.theWorld.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y + 1.0D, z,
                                (rnd.nextDouble() - 0.5D) * 0.5D, rnd.nextDouble() * 0.4D, (rnd.nextDouble() - 0.5D) * 0.5D);
                    }
                    break;
                case 3:
                    for (int i = 0; i < 14; i++) {
                        mc.theWorld.spawnParticle(EnumParticleTypes.HEART, x + (rnd.nextDouble() - 0.5D), y + 0.5D + rnd.nextDouble() * 1.5D,
                                z + (rnd.nextDouble() - 0.5D), 0.0D, 0.0D, 0.0D);
                    }
                    break;
                default:
                    int id = Block.getStateId(Blocks.redstone_block.getDefaultState());
                    for (int i = 0; i < 40; i++) {
                        mc.theWorld.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y + 1.0D, z,
                                (rnd.nextDouble() - 0.5D) * 0.4D, rnd.nextDouble() * 0.3D, (rnd.nextDouble() - 0.5D) * 0.4D, id);
                    }
            }
            if (sound.b) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("random.levelup"), 1.0F));
            }
        }
    }

    // ------------------------------------------------------------ Damage Popups

    static class DamagePopups extends Module {
        static final class Pending {
            EntityLivingBase e;
            float before;
            int ticks;
        }

        static final class Pop {
            double x, y, z;
            long born;
            String text;
        }

        final Setting life = Setting.slider("life", "Duration (s)", 1.2F, 0.5F, 3.0F, 0.1F);
        final Setting size = Setting.slider("size", "Size", 1.0F, 0.5F, 2.0F, 0.1F);
        final Setting color = Setting.color("color", "Colour", 1);
        private final List<Pending> pending = new ArrayList<Pending>();
        private final List<Pop> pops = new ArrayList<Pop>();
        private final Random rnd = new Random();

        DamagePopups() {
            super("damagepopups", "Damage Popups", "Floating numbers with the damage you deal.", "PvP");
            settings.add(life);
            settings.add(size);
            settings.add(color);
        }

        @Override
        public void attacked(Entity t) {
            if (!(t instanceof EntityLivingBase)) return;
            EntityLivingBase e = (EntityLivingBase) t;
            for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
                if (it.next().e == e) it.remove();
            }
            Pending p = new Pending();
            p.e = e;
            p.before = e.getHealth();
            p.ticks = 10;
            pending.add(p);
            if (pending.size() > 16) pending.remove(0);
        }

        @Override
        public void onTick() {
            for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
                Pending p = it.next();
                float now = p.e.getHealth();
                if (now < p.before - 0.01F) {
                    Pop pop = new Pop();
                    pop.x = p.e.posX + (rnd.nextDouble() - 0.5D) * 0.6D;
                    pop.y = p.e.posY + p.e.height + 0.1D;
                    pop.z = p.e.posZ + (rnd.nextDouble() - 0.5D) * 0.6D;
                    pop.born = System.currentTimeMillis();
                    pop.text = Q.f("-%.1f", p.before - now);
                    pops.add(pop);
                    it.remove();
                } else if (--p.ticks <= 0 || p.e.isDead) {
                    it.remove();
                }
            }
        }

        @Override
        public void renderWorld(float pt) {
            if (pops.isEmpty()) return;
            RenderManager rm = mc.getRenderManager();
            FontRenderer fr = mc.fontRendererObj;
            long now = System.currentTimeMillis();
            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            for (Iterator<Pop> it = pops.iterator(); it.hasNext(); ) {
                Pop p = it.next();
                float age = (now - p.born) / (life.v * 1000.0F);
                if (age >= 1.0F) {
                    it.remove();
                    continue;
                }
                float a = age < 0.6F ? 1.0F : (1.0F - age) / 0.4F;
                GlStateManager.pushMatrix();
                GlStateManager.translate(p.x - rm.viewerPosX, p.y + age * 0.8D - rm.viewerPosY, p.z - rm.viewerPosZ);
                GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                float s = 0.03F * size.v;
                GlStateManager.scale(-s, -s, s);
                int al = (int) (a * 255.0F);
                if (al > 8) fr.drawStringWithShadow(p.text, -fr.getStringWidth(p.text) / 2, -4, (al << 24) | color.rgb());
                GlStateManager.popMatrix();
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    // ------------------------------------------------------------ Target Gear

    /** The armor and weapon of the player you aim at, with durability. */
    static class TargetGear extends Module {
        final Setting dur = Setting.bool("dur", "Show durability %", true);
        private EntityPlayer target;
        private long seen;

        TargetGear() {
            super("targetgear", "Target Gear", "Armor and weapon of the player you aim at.", "PvP");
            asHud(0.72F, 0.4F, 0);
            settings.add(dur);
            moved = true;
            background.b = true;
        }

        @Override
        public void onTick() {
            MovingObjectPosition o = mc.objectMouseOver;
            if (o != null && o.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && o.entityHit instanceof EntityPlayer) {
                target = (EntityPlayer) o.entityHit;
                seen = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - seen > 2500L || (target != null && target.isDead)) {
                target = null;
            }
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final EntityPlayer t = target;
            if (t == null && !editing) return;
            final List<ItemStack> items = new ArrayList<ItemStack>();
            if (t != null) {
                for (int slot = 4; slot >= 0; slot--) {
                    ItemStack s = t.getEquipmentInSlot(slot);
                    if (s != null) items.add(s);
                }
            } else {
                items.add(new ItemStack(Items.diamond_helmet));
                items.add(new ItemStack(Items.diamond_chestplate));
                items.add(new ItemStack(Items.diamond_leggings));
                items.add(new ItemStack(Items.diamond_boots));
                items.add(new ItemStack(Items.diamond_sword));
            }
            final String name = t == null ? "Player" : t.getName();
            Hud.box(this, sw, sh, 84, 14 + Math.max(1, items.size()) * 18, new Hud.Painter() {
                @Override
                public void paint(Module m, FontRenderer fr) {
                    Hud.text(m, fr, fr.trimStringToWidth(name, 78), 3, 3);
                    RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.enableRescaleNormal();
                    for (int i = 0; i < items.size(); i++) {
                        ItemStack s = items.get(i);
                        int y = 13 + i * 18;
                        mc.getRenderItem().renderItemAndEffectIntoGUI(s, 3, y);
                        int pct = Q.durabilityPercent(s);
                        if (dur.b && pct >= 0) Hud.text(m, fr, pct + "%", 24, y + 5);
                    }
                    GlStateManager.disableRescaleNormal();
                    RenderHelper.disableStandardItemLighting();
                }
            });
        }
    }
}
