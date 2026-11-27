package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.YAHud;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.adryh01.yafabric.mods.Extra;

/** Draws the YA HUD on top of the normal in-game HUD and ticks the modules. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render(F)V", at = @At("RETURN"))
    private void yaHud(float tickDelta, CallbackInfo ci) {
        YAHud.render();
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void yaTick(CallbackInfo ci) {
        YAHud.tick();
    }

    @Inject(method = "showCrosshair()Z", at = @At("HEAD"), cancellable = true)
    private void yaCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Extra.crosshairHidden()) cir.setReturnValue(Boolean.FALSE);
    }
}
