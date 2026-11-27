package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.Panels;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The YA chat panel draws the chat instead of the vanilla one. */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "render(I)V", at = @At("HEAD"), cancellable = true)
    private void yaChat(int ticks, CallbackInfo ci) {
        if (Panels.hideVanillaChat()) ci.cancel();
    }
}
