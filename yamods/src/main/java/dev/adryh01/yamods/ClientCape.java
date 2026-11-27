package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;
import java.util.Map;

/**
 * The YA CLIENT cape: a white 3D cape with a thick white edge and the black YA logo in the middle.
 * Every player who uses YA CLIENT wears it (the same check as the nick tag), and everybody who uses it sees it.
 */
public class ClientCape extends Module {

    static final ResourceLocation TEX = new ResourceLocation("yamods", "textures/ya_cape.png");
    static final Model MODEL = new Model();

    final Setting own = Setting.bool("own", "Show my cape", true);
    final Setting others = Setting.bool("others", "Show the cape of other YA players", true);

    ClientCape() {
        super("clientcape", "YA Cape", "The white 3D YA CLIENT cape that every YA player wears.", "Visual");
        settings.add(own);
        settings.add(others);
        enabled = true;
    }

    /** Exactly the vanilla cape box (10 x 16 x 1); only the texture is ours. */
    static final class Model extends ModelBase {
        final ModelRenderer cape;

        Model() {
            textureWidth = 64; // the PNG is bigger, the UV is relative, so the logo stays sharp
            textureHeight = 32;
            cape = new ModelRenderer(this, 0, 0);
            cape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, 0.0F);
        }

        void render(float scale) {
            cape.render(scale);
        }
    }

    /** Draws our cape for YA players and the normal cape for everybody else. */
    static final class Layer implements LayerRenderer<AbstractClientPlayer> {
        final RenderPlayer render;
        final LayerRenderer<AbstractClientPlayer> orig;

        Layer(RenderPlayer render, LayerRenderer<AbstractClientPlayer> orig) {
            this.render = render;
            this.orig = orig;
        }

        private boolean ours(AbstractClientPlayer p) {
            Module m = Mods.byId("clientcape");
            if (m == null || !m.enabled) return false;
            ClientCape c = (ClientCape) m;
            boolean self = p == Minecraft.getMinecraft().thePlayer;
            if (self ? !c.own.b : !c.others.b) return false;
            return YaNet.hasCape(p.getUniqueID()); // only players who own the cape (claimed free on the website)
        }

        @Override
        public void doRenderLayer(AbstractClientPlayer p, float a, float b, float pt, float d, float e, float f, float scale) {
            if (!ours(p)) {
                if (orig != null) orig.doRenderLayer(p, a, b, pt, d, e, f, scale);
                return;
            }
            if (p.isInvisible()) return;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TEX);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 0.125F);
            double d0 = p.prevChasingPosX + (p.chasingPosX - p.prevChasingPosX) * (double) pt - (p.prevPosX + (p.posX - p.prevPosX) * (double) pt);
            double d1 = p.prevChasingPosY + (p.chasingPosY - p.prevChasingPosY) * (double) pt - (p.prevPosY + (p.posY - p.prevPosY) * (double) pt);
            double d2 = p.prevChasingPosZ + (p.chasingPosZ - p.prevChasingPosZ) * (double) pt - (p.prevPosZ + (p.posZ - p.prevPosZ) * (double) pt);
            float yaw = p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * pt;
            double sin = (double) MathHelper.sin(yaw * (float) Math.PI / 180.0F);
            double cos = (double) (-MathHelper.cos(yaw * (float) Math.PI / 180.0F));
            float lift = (float) d1 * 10.0F;
            lift = MathHelper.clamp_float(lift, -6.0F, 32.0F);
            float back = (float) (d0 * sin + d2 * cos) * 100.0F;
            float side = (float) (d0 * cos - d2 * sin) * 100.0F;
            if (back < 0.0F) back = 0.0F;
            float bob = p.prevCameraYaw + (p.cameraYaw - p.prevCameraYaw) * pt;
            lift += MathHelper.sin((p.prevDistanceWalkedModified + (p.distanceWalkedModified - p.prevDistanceWalkedModified) * pt) * 6.0F) * 32.0F * bob;
            if (p.isSneaking()) lift += 25.0F;
            GlStateManager.rotate(6.0F + back / 2.0F + lift, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(side / 2.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(-side / 2.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            MODEL.render(0.0625F);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /** Swaps the vanilla cape layer of both player renderers (Steve and Alex) for ours. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void installAll() {
        try {
            Map<String, RenderPlayer> skins = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            for (RenderPlayer rp : skins.values()) {
                List list = ReflectionHelper.getPrivateValue(net.minecraft.client.renderer.entity.RendererLivingEntity.class, rp, "layerRenderers", "field_177097_h");
                LayerRenderer orig = null;
                int idx = -1;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof LayerCape) {
                        orig = (LayerRenderer) list.get(i);
                        idx = i;
                        break;
                    }
                }
                Layer layer = new Layer(rp, orig);
                if (idx >= 0) list.set(idx, layer);
                else list.add(layer);
            }
        } catch (Throwable t) {
            System.out.println("[YA MODS] could not install the YA cape: " + t);
        }
    }
}
