package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.YAHome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every time the game opens the vanilla title screen, open the YA home instead. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"), argsOnly = true)
    private Screen yaReplaceTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class) {
            return new YAHome();
        }
        return screen;
    }
}
