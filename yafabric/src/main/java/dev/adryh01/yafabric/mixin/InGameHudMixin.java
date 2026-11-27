package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.YAHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import net.minecraft.scoreboard.ScoreboardObjective;
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

    @Inject(method = "renderScoreboardObjective(Lnet/minecraft/scoreboard/ScoreboardObjective;Lnet/minecraft/client/util/Window;)V", at = @At("HEAD"), cancellable = true)
    private void yaBoard(ScoreboardObjective objective, Window window, CallbackInfo ci) {
        if (dev.adryh01.yafabric.mods.Panels.hideVanillaBoard()) ci.cancel();
    }
}
