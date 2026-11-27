package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.NickTag;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nick Tag: the nickname and the YA logo above the head, drawn after the entity. */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("RETURN"))
    private void yaTag(LivingEntity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        try {
            NickTag.label(entity, x, y, z);
        } catch (RuntimeException e) {
            // never break the rendering of the game
        }
    }
}
