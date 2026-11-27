package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.Extra;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom (field of view) and Freelook (camera rotation swap around a frame). */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render(FJ)V", at = @At("HEAD"))
    private void yaFrameStart(float tickDelta, long nanoTime, CallbackInfo ci) {
        Extra.frameStart();
    }

    @Inject(method = "render(FJ)V", at = @At("RETURN"))
    private void yaFrameEnd(float tickDelta, long nanoTime, CallbackInfo ci) {
        Extra.frameEnd();
    }

    @Inject(method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
    private void yaFov(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float m = Extra.fovMultiplier();
        if (m != 1.0F) cir.setReturnValue(cir.getReturnValue() * m);
    }
}
