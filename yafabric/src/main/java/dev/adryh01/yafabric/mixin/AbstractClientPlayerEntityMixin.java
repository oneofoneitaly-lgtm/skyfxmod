package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.CapeNet;
import dev.adryh01.yafabric.mods.Module;
import dev.adryh01.yafabric.mods.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players who claimed the free YA Cape get it, rendered by the normal cape layer. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    private static final Identifier YA_CAPE = new Identifier("yafabric", "textures/ya_cape.png");

    private boolean yaOwns() {
        Module m = Mods.byId("clientcape");
        if (m == null || !m.enabled) return false;
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean me = mc.player != null && self.getUuid().equals(mc.player.getUuid());
        if (me ? !m.setting("mine").b : !m.setting("others").b) return false;
        return CapeNet.hasCape(self.getUuid());
    }

    @Inject(method = "getCapeId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void yaCapeId(CallbackInfoReturnable<Identifier> cir) {
        if (yaOwns()) cir.setReturnValue(YA_CAPE);
    }

    @Inject(method = "canRenderCapeTexture()Z", at = @At("HEAD"), cancellable = true)
    private void yaCanRender(CallbackInfoReturnable<Boolean> cir) {
        if (yaOwns()) cir.setReturnValue(Boolean.TRUE);
    }
}
