package dev.adryh01.yafabric.mods;

import dev.adryh01.yafabric.YaNet;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

/** Nickname above your head (client side) and the white YA logo next to the name tags. */
public final class NickTag extends Module {

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

    public static boolean announceOn() {
        Module n = Mods.byId("nicktag");
        return !(n instanceof NickTag) || ((NickTag) n).announce.b;
    }

    /** Your own nickname when the module is on and one is set, else null. */
    static String myNick() {
        Module n = Mods.byId("nicktag");
        return n instanceof NickTag && n.enabled && ((NickTag) n).nick.s.length() > 0 ? ((NickTag) n).nick.s : null;
    }

    /** Called after every living entity was drawn (camera space, x/y/z relative to the camera). */
    public static void label(LivingEntity e, double x, double y, double z) {
        Module m = Mods.byId("nicktag");
        if (m instanceof NickTag && m.enabled) ((NickTag) m).draw(e, x, y, z);
    }

    private void draw(LivingEntity e, double x, double y, double z) {
        if (!(e instanceof PlayerEntity) || mc.player == null || mc.world == null) return;
        PlayerEntity pl = (PlayerEntity) e;
        boolean self = pl == mc.player;
        if (self) {
            if (mc.options.perspective == 0) return; // you never see your own tag in first person
        } else {
            if (who.idx == 0 || !logo.b || !YaNet.uses(pl.getUuid()) || pl.isSneaking() || pl.isInvisibleTo(mc.player)) return;
            if (pl.squaredDistanceTo(mc.player) >= 64.0D * 64.0D) return;
        }
        EntityRenderDispatcher rm = mc.getEntityRenderManager();
        TextRenderer fr = mc.textRenderer;
        String name = self && nick.s.length() > 0 ? nick.s : pl.getName().asFormattedString();
        float f = 0.026666668F;
        double yy = y + pl.height + 0.5D;
        if (!self && pl.squaredDistanceTo(mc.player) < 100.0D && mc.world.getScoreboard().getObjectiveForSlot(2) != null) {
            yy += fr.fontHeight * 1.15F * f; // vanilla lifts the name when a score is shown below it
        }
        int st = Gl.save();
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) yy, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rm.yaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.pitch * (mc.options.perspective == 2 ? -1.0F : 1.0F), 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-f, -f, f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int tw = fr.getStringWidth(name) / 2;
        int lw = logo.b ? Logo.width(10) + 3 : 0;
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) GL11.glDisable(GL11.GL_DEPTH_TEST);
            else GL11.glEnable(GL11.GL_DEPTH_TEST);
            float a = pass == 0 ? 0.25F : 1.0F;
            if (self && pass == 0) {
                int s2 = Gl.save();
                Gl.flat();
                Gl.rect(-tw - 1 - lw, -1, tw + 1, 8, 0x40000000);
                Gl.restore(s2 | 1);
                GL11.glDepthMask(false);
                GL11.glDisable(GL11.GL_LIGHTING);
            }
            if (logo.b) Logo.draw(-tw - lw, -1, 10, a);
            if (self) fr.draw(name, -tw, 0, pass == 0 ? 0x20FFFFFF : -1);
        }
        GL11.glPopMatrix();
        Gl.restore(st);
    }
}
