package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.Pvp;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hitbox (after the entities) and Block outline (replaces the vanilla black box). */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/CameraView;F)V", at = @At("RETURN"))
    private void yaEntities(Entity camera, CameraView view, float tickDelta, CallbackInfo ci) {
        Pvp.world(tickDelta);
    }

    @Inject(method = "drawBlockOutline(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;IF)V", at = @At("HEAD"), cancellable = true)
    private void yaOutline(PlayerEntity player, BlockHitResult hit, int pass, float tickDelta, CallbackInfo ci) {
        if (Pvp.outline(player, hit, tickDelta)) ci.cancel();
    }
}
