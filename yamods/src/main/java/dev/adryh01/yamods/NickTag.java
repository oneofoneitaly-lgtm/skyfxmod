package dev.adryh01.yamods;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/** Nickname above your head (client side) and the white YA logo next to the name tags. */
final class NickTag extends Module {

    final Setting nick = Setting.text("nick", "Nickname (empty = real name)", "", 16);
    final Setting who = Setting.choice("who", "Logo on", 1, "Only me", "YA users");
    final Setting logo = Setting.bool("logo", "Logo next to the name", true);
    final Setting announce = Setting.bool("announce", "Tell YA servers I use YA MODS", true);

    NickTag() {
        super("nicktag", "Nick Tag", "Your nickname and the YA logo above the head of every YA user (found through the YA CLIENT server, no plugin needed).", "Visual");
        settings.add(nick);
        settings.add(who);
        settings.add(logo);
        settings.add(announce);
    }

    /** Called for every living entity label (before the vanilla one). Nothing is cancelled: the vanilla tag stays. */
    void label(EntityLivingBase e, double x, double y, double z) {
        if (!(e instanceof EntityPlayer) || mc.thePlayer == null) return;
        EntityPlayer pl = (EntityPlayer) e;
        boolean self = pl == mc.thePlayer;
        if (self) {
            if (mc.gameSettings.thirdPersonView == 0) return; // you never see your own tag in first person
        } else {
            if (who.idx == 0 || !logo.b || !YaNet.uses(pl.getUniqueID()) || pl.isSneaking() || pl.isInvisibleToPlayer(mc.thePlayer)) return;
            double d2 = pl.getDistanceSqToEntity(mc.thePlayer);
            if (d2 >= 64.0D * 64.0D) return;
        }
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = mc.fontRendererObj;
        String name = self && nick.s.length() > 0 ? nick.s : pl.getDisplayName().getFormattedText();
        float f = 0.026666668F;
        double yy = y + pl.height + 0.5D;
        if (!self && pl.getDistanceSqToEntity(mc.thePlayer) < 100.0D && mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(2) != null) {
            yy += fr.FONT_HEIGHT * 1.15F * f; // vanilla lifts the name when a score is shown below it
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) yy, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX * (mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F), 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f, -f, f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int tw = fr.getStringWidth(name) / 2;
        int lw = logo.b ? Logo.width(10) + 3 : 0;
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) GlStateManager.disableDepth();
            else GlStateManager.enableDepth();
            float a = pass == 0 ? 0.25F : 1.0F;
            if (self && pass == 0) Gui.drawRect(-tw - 1 - lw, -1, tw + 1, 8, 0x40000000);
            if (logo.b) Logo.draw(-tw - lw, -1, 10, a);
            if (self) {
                GlStateManager.enableBlend();
                fr.drawString(name, -tw, 0, pass == 0 ? 0x20FFFFFF : -1);
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
