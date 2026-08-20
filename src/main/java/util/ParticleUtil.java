package util;

import dev.amble.timelordregen.core.particle_effects.RegenParticleEffect;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ParticleUtil {
    private final boolean hasHead;
    public ParticleUtil(boolean hasHead) {
        this.hasHead = hasHead;
    }

    public void spawnParticles(LivingEntity entity, ServerWorld serverWorld) {
        if (!(serverWorld.getBlockState(BlockPos.ofFloored(entity.getPos())).getBlock() instanceof StonecutterBlock)) return;

        float yaw = entity.getYaw() * ((float) Math.PI / 180F);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double leftX = entity.getX() + cos * -0.8f - sin * 0;
        double leftZ = entity.getZ() + sin * -0.8f + cos * 0;
        serverWorld.spawnParticles(new RegenParticleEffect(entity.getId(), 0, 0, false, true, 0.4f), leftX, entity.getY() + 1.25f, leftZ,
                100, 0, 0, 0, 1);

        // Head particle (centered)
        if (hasHead) {
            float pitchRadians = entity.getPitch() * ((float) Math.PI / 180F);
            double yOffset = 1.5f + Math.sin(-pitchRadians) * 0.5f;
            serverWorld.spawnParticles(new RegenParticleEffect(entity.getId(), 0, 0, true, true, 0.4f), entity.getX(), entity.getY() + yOffset, entity.getZ(),
                    100, 0.1, 0, 0.1, 1);
        }

        //  Right particle (relative to facing)
        double rightX = entity.getX() - cos * -0.8f - sin * 0;
        double rightZ = entity.getZ() - sin * -0.8f + cos * 0;
        serverWorld.spawnParticles(new RegenParticleEffect(entity.getId(), 0, 0, false, true, 0.4f), rightX, entity.getY() + 1.25f, rightZ,
                100, 0, 0, 0, 1);
    }
}
