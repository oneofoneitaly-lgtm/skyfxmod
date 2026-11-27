package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.YAHud;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the YA HUD on top of the normal in-game HUD. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render(F)V", at = @At("RETURN"))
    private void yaHud(float tickDelta, CallbackInfo ci) {
        YAHud.render();
    }
}
