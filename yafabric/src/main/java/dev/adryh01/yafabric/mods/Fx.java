package dev.adryh01.yafabric.mods;

import net.minecraft.client.particle.ParticleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;

import java.util.List;
import java.util.Random;

/** Particle modules: Hit Sparks and YA Trail. */
public final class Fx {

    static final HitSparks SPARKS = new HitSparks();

    private Fx() {
    }

    static void register(List<Module> all) {
        all.add(SPARKS);
        all.add(new Trail());
    }

    /** Called when the local player attacks something (ClientPlayerInteractionManager mixin). */
    public static void attacked(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        Q.hit();
        if (SPARKS.enabled) SPARKS.spark(target);
    }

    static final class HitSparks extends Module {
        HitSparks() {
            super("hitsparks", "Hit Sparks", "Crit sparks on every player or mob you hit.", "PvP");
        }

        void spark(Entity target) {
            if (mc.player == null) return;
            mc.player.addCritParticles(target);
            mc.player.addEnchantedHitParticles(target);
        }
    }

    static final class Trail extends Module {
        final Setting density = Setting.slider("trail_density", "Density", 6.0F, 1.0F, 12.0F, 1.0F);
        private final Random rnd = new Random();

        Trail() {
            super("trail", "YA Trail", "A sparkle trail behind you while you move.", "Visual");
            settings.add(density);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (p == null || mc.world == null) return;
            double dx = p.x - p.prevX, dz = p.z - p.prevZ;
            if (dx * dx + dz * dz < 0.0004D) return;
            int n = Math.max(1, (int) (density.v / 3.0F));
            for (int i = 0; i < n; i++) {
                mc.world.addParticle(ParticleType.FIREWORK_SPARK,
                        p.x + (rnd.nextDouble() - 0.5D) * 0.5D, p.y + 0.1D + rnd.nextDouble() * 0.3D,
                        p.z + (rnd.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
