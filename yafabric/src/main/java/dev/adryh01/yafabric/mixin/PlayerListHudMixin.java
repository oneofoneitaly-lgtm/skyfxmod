package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.Panels;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The YA tab list is drawn instead of the vanilla one. */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "render(ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
    private void yaTab(int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (Panels.hideVanillaTab()) ci.cancel();
    }
}
