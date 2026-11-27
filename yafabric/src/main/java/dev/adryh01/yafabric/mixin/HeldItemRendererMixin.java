package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.Pvp;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.7 animations: swing the arm while blocking / eating / drawing a bow. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderArmHoldingItem(F)V", at = @At("HEAD"))
    private void yaHandStart(float tickDelta, CallbackInfo ci) {
        Pvp.handBegin();
    }

    @Inject(method = "renderArmHoldingItem(F)V", at = @At("RETURN"))
    private void yaHandEnd(float tickDelta, CallbackInfo ci) {
        Pvp.handEnd();
    }
}
