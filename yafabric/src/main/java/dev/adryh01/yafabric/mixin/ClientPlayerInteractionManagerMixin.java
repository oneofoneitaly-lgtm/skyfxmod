package dev.adryh01.yafabric.mixin;

import dev.adryh01.yafabric.mods.Fx;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every time you attack something: hit counter, combo and Hit Sparks. */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void yaAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        try {
            Fx.attacked(target);
        } catch (RuntimeException e) {
            System.out.println("[YA CLIENT] attack hook: " + e);
        }
    }
}
