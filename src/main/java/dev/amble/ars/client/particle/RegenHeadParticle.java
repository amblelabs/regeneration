package dev.amble.ars.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class RegenHeadParticle extends ExplosionSmokeParticle {
    private final SpriteProvider spriteProvider;
    private static final int TOTAL_FRAMES = 7;

    RegenHeadParticle(ClientWorld clientWorld, double d, double e, double f, double velX, double velY, double velZ, SpriteProvider spriteProvider) {
        super(
                clientWorld,
                d + (Math.random() * 0.4 - 0.2),
                e + (Math.random() * 0.4 - 0.2),
                f + (Math.random() * 0.4 - 0.2),
                0, 0, 0, spriteProvider
        );
        this.spriteProvider = spriteProvider;
        this.gravityStrength = 0.01f;
        this.velocityMultiplier = 0.999f;
        this.velocityX = velocityX + (Math.random() * 2.0 - 1.0) * 0.1000000074505806;
        this.velocityY = velocityY + (Math.random() * 2.0 - 1.0) * 0.05000000074505806;
        this.velocityZ = velocityZ + (Math.random() * 2.0 - 1.0) * 0.1000000074505806;
        this.velocityX += velocityX;
        this.velocityY += velocityY;
        this.velocityZ += velocityZ;
        this.angle = (float) Math.random() * 6.2831855F;
        this.prevAngle = this.angle;
        this.alpha = 1f;
        this.velocityY = this.random.nextFloat() * 0.2F + 0.6F;
        this.scale *= 0.5f;
        this.setColor(1f, 0.9f, 0.9f);

        this.maxAge = 30;
        this.collidesWithWorld = true;

        // ================================================================
        // 强制初始帧为第 0 帧
        // getSprite(0, 1000) → 0 * 6 / 1000 = 0，返回 sprites[0]
        // ================================================================
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
            // 手动顺序切帧：每 tick 严格推进一帧
            // getSprite(frame, 6) → frame * 6 / 6 = frame，直接取 sprites[frame]
            // ================================================================
            int frame = (this.age - 1) % TOTAL_FRAMES;
            this.setSprite(this.spriteProvider.getSprite(frame, TOTAL_FRAMES - 1));
        }
        if (!(this.alpha <= 0.0F)) {
            if (this.alpha > 0.01F) {
                this.alpha -= 0.01F;
            }
        } else {
            this.markDead();
        }
    }
    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
            return new RegenHeadParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
        }
    }
}