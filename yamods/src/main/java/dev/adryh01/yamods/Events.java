package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge events that drive the modules. */
public class Events {

    private final Minecraft mc = Minecraft.getMinecraft();
    private int saveTimer;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        if (YAMods.menuKey.isPressed() && mc.currentScreen == null && mc.thePlayer != null) {
            mc.displayGuiScreen(new ModMenu());
        }
        if (++saveTimer >= 600) { // every 30 seconds, only writes when something changed
            saveTimer = 0;
            Mods.save();
        }
        if (mc.thePlayer == null) return;
        Q.tick(mc.thePlayer);
        for (Module m : Mods.ALL) {
            if (m.enabled) m.onTick();
        }
        // let the zoom ease out even after the module is switched off
        if (!Mods.ZOOM.enabled) Mods.ZOOM.onDisable();
    }

    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        Module c = Mods.byId("crosshair");
        if (c != null && c.enabled) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.thePlayer == null || mc.gameSettings.showDebugInfo || mc.gameSettings.hideGUI) return;
        if (mc.currentScreen instanceof HudEditor) return; // the editor draws the HUD itself
        ScaledResolution sr = new ScaledResolution(mc);
        Hud.renderAll(sr.getScaledWidth(), sr.getScaledHeight(), false);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (mc.currentScreen != null) return;
        if (e.buttonstate && e.button == 0) Mods.click(true);
        if (e.buttonstate && e.button == 1) Mods.click(false);

        Mods.Zoom z = Mods.ZOOM;
        if (z.enabled && z.zooming && z.scroll.b && e.dwheel != 0) {
            z.amount.setFraction(z.amount.fraction() + Integer.signum(e.dwheel) * 0.06F);
            e.setCanceled(true); // do not change the hotbar slot while zooming
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer != mc.thePlayer || !(e.target instanceof EntityLivingBase)) return;
        Q.hit(e.target);
        for (Module m : Mods.ALL) {
            if (m.enabled) m.attacked(e.target);
        }
        Module sp = Mods.byId("hitsparks");
        if (sp != null && sp.enabled) ((Fx.HitSparks) sp).spark(e.target);
        Mods.lastReach = e.entityPlayer.getDistanceToEntity(e.target);
        if (mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null && mc.objectMouseOver.entityHit == e.target) {
            Mods.lastReach = (float) e.entityPlayer.getPositionEyes(1.0F).distanceTo(mc.objectMouseOver.hitVec);
        }
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        Mods.Zoom z = Mods.ZOOM;
        Module nf = Mods.byId("nofoveffects");
        if (nf != null && nf.enabled) e.newfov = 1.0F;
        if (z.enabled && z.anim > 0.0F) e.newfov = e.newfov * z.fovMultiplier();
    }

    @SubscribeEvent
    public void onOverlayBlock(RenderBlockOverlayEvent e) {
        Module m = Mods.byId("nofire");
        if (m != null && m.enabled && e.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldLast(RenderWorldLastEvent e) {
        if (mc.thePlayer == null) return;
        for (Module m : Mods.ALL) {
            if (m.enabled) m.renderWorld(e.partialTicks);
        }
    }

    @SubscribeEvent
    public void onBlockHighlight(DrawBlockHighlightEvent e) {
        for (Module m : Mods.ALL) {
            if (m.enabled && m.drawBlock(e)) {
                e.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup e) {
        for (Module m : Mods.ALL) {
            if (m.enabled) m.camera(e);
        }
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent e) {
        for (Module m : Mods.ALL) {
            if (m.enabled) m.renderHand(e.partialTicks);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        for (Module m : Mods.ALL) {
            if (m.enabled) m.chat(e);
        }
    }

    /** Leaving a server (or switching to another one) saves everything. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        Mods.save();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (e.world.isRemote) Mods.save();
    }
}
