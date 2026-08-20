package dev.amble.timelordregen.client.particle;

import dev.amble.timelordregen.core.particle_effects.RegenParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RightRegenParticle extends ExplosionSmokeParticle {
    private final SpriteProvider spriteProvider;

    public RightRegenParticle(ClientWorld clientWorld, double d, double e, double f, SpriteProvider spriteProvider, Entity entity,
                       float yawOffset, float pitchOffset, boolean shouldPitch, boolean shouldFollowPlayer, float speed) {
        super(clientWorld, d, e, f, 0, 0, 0, spriteProvider);
        this.spriteProvider = spriteProvider;

        if (!(entity instanceof LivingEntity living)) return;
        this.gravityStrength = 0.01f;
        this.velocityMultiplier = 0.999f;

        // When shouldFollowPlayer is true, yawOffset/pitchOffset are relative to player's look direction
        // When shouldFollowPlayer is false, yawOffset/pitchOffset are absolute world rotations
        float yawRad = shouldFollowPlayer
            ? (float) Math.toRadians(living.getHeadYaw() + yawOffset)
            : (float) Math.toRadians(yawOffset);
        float pitchRad = shouldPitch
            ? (float) Math.toRadians(shouldFollowPlayer ? (living.getPitch() + pitchOffset) : pitchOffset)
            : 0f;

        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = shouldPitch ? -Math.sin(pitchRad) : 0;
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double randX = Math.random() * 0.06 - 0.04;
        double randY = Math.random() * 0.06 - 0.04;
        double randZ = Math.random() * 0.06 - 0.04;
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length != 0) {
            dirX /= length;
            dirY /= length;
            dirZ /= length;
        }
        this.velocityX = dirX * speed + randX;
        this.velocityY = dirY * speed + randY;
        this.velocityZ = dirZ * speed + randZ;

        this.angle = (float) Math.random() * 6.2831855F;
        this.prevAngle = this.angle;
        this.alpha = 0.4f + (float) Math.random() * 0.2f;
        this.scale *= 0.5f;
        this.setColor(1f, 0.9f, 0.9f);
        this.maxAge = this.random.nextInt(4) + 10;
        this.collidesWithWorld = true;
    }

    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public int getBrightness(float tint) {
        int i = super.getBrightness(tint);
        int k = i >> 16 & 255;
        return 240 | k << 16;
    }

    public void tick() {
        super.tick();
        if (!this.dead) {
            this.setSpriteForAge(this.spriteProvider);
        }
        if (!(this.alpha <= 0.0F)) {
            if (this.alpha > 0.01F) {
                this.alpha -= 0.03F;
            }
        } else {
            this.markDead();
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<RegenParticleEffect> {
        private final SpriteProvider spriteProvider;
        private static SpriteProvider staticSpriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
            staticSpriteProvider = spriteProvider;
        }

        public static SpriteProvider getSpriteProvider() {
            return staticSpriteProvider;
        }

        @Override
        public @Nullable Particle createParticle(RegenParticleEffect regenParticle, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            RightRegenParticle rightRegenParticle = new RightRegenParticle(world, x, y, z, this.spriteProvider,
                    regenParticle.getEntity(world), regenParticle.getYawOffset(), regenParticle.getPitchOffset(), regenParticle.getShouldPitch(), regenParticle.getShouldFollowPlayer(), regenParticle.getSpeed());
            rightRegenParticle.setSprite(this.spriteProvider);
            return rightRegenParticle;
        }
    }
}
