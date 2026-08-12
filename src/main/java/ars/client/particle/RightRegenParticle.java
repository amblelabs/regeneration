package ars.client.particle;

import ars.core.particle_effects.RegenParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RightRegenParticle extends ExplosionSmokeParticle {
    private final SpriteProvider spriteProvider;
    private static final int TOTAL_FRAMES = 7;

    public RightRegenParticle(ClientWorld clientWorld, double d, double e, double f,
                              double velX, double velY, double velZ,
                              SpriteProvider spriteProvider, Entity entity,
                              float yawOffset, float pitchOffset, boolean shouldPitch, boolean shouldFollowPlayer,
                              float speed, boolean shortLife) {
        super(clientWorld, d, e, f, 0, 0, 0, spriteProvider);
        this.spriteProvider = spriteProvider;
        this.gravityStrength = 0.01f;
        this.velocityMultiplier = 0.999f;

        double dirX, dirY, dirZ;

        if (!shouldFollowPlayer) {
            dirX = velX;
            dirY = velY;
            dirZ = velZ;
        } else if (entity instanceof PlayerEntity player) {
            float yawRad = (float) Math.toRadians(player.headYaw + yawOffset);
            float pitchRad = shouldPitch ? (float) Math.toRadians(shouldFollowPlayer ? (player.getPitch() + pitchOffset) : pitchOffset) : 0f;
            dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
            dirY = shouldPitch ? -Math.sin(pitchRad) : 0;
            dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        } else if (entity instanceof LivingEntity living) {
            float yawRad = (float) Math.toRadians(living.getYaw() + yawOffset);
            float pitchRad = shouldPitch ? (float) Math.toRadians(pitchOffset) : 0f;
            dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
            dirY = shouldPitch ? -Math.sin(pitchRad) : 0;
            dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        } else {
            dirX = 0; dirY = 0; dirZ = 0;
        }

        if (!shouldFollowPlayer) {
            this.velocityX = dirX + (Math.random() - 0.5) * 0.005;
            this.velocityY = dirY + (Math.random() - 0.5) * 0.005;
            this.velocityZ = dirZ + (Math.random() - 0.5) * 0.005;
        } else {
            double randX = Math.random() * 0.06 - 0.04;
            double randY = Math.random() * 0.06 - 0.04;
            double randZ = Math.random() * 0.06 - 0.04;
            double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (length != 0) {
                dirX /= length; dirY /= length; dirZ /= length;
            }
            this.velocityX = dirX * speed + randX;
            this.velocityY = dirY * speed + randY;
            this.velocityZ = dirZ * speed + randZ;
        }

        this.angle = (float) Math.random() * 6.2831855F;
        this.prevAngle = this.angle;
        this.alpha = 0.4f + (float) Math.random() * 0.2f;
        this.scale *= 0.5f;
        this.setColor(1f, 0.9f, 0.9f);

        // ================================================================
        // 确保 maxAge >= TOTAL_FRAMES，否则手动切帧会循环或跳帧
        // ================================================================
        if (shortLife) {
            this.maxAge = this.random.nextInt(3) + 7; // 7~9
        } else {
            this.maxAge = this.random.nextInt(6) + 7; // 7~12
        }
        this.collidesWithWorld = true;

        // 强制初始帧为第 0 帧，覆盖基类随机选图
        this.setSprite(spriteProvider.getSprite(0, 1000));
    }

    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880; // 0xF000F0，全亮，不受场景光照影响
    }

    public void tick() {
        super.tick();
        if (!this.dead) {
            // ================================================================
            // 手动顺序切帧，严格从 0 → 6 循环
            // getSprite(frame, 6) → frame * 6 / 6 = frame
            // ================================================================
            int frame = (this.age - 1) % TOTAL_FRAMES;
            this.setSprite(this.spriteProvider.getSprite(frame, TOTAL_FRAMES - 1));
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
            RightRegenParticle p = new RightRegenParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider,
                    regenParticle.getEntity(world), regenParticle.getYawOffset(), regenParticle.getPitchOffset(),
                    regenParticle.getShouldPitch(), regenParticle.getShouldFollowPlayer(), regenParticle.getSpeed(),
                    regenParticle.isShortLife());

            // ================================================================
            // 移除 p.setSprite(this.spriteProvider)，它调用的是 getSprite(Random)
            // 构造函数里已经强制 setSprite(0, 1000) 为第 0 帧
            // ================================================================
            return p;
        }
    }
}