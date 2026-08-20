package dev.amble.timelordregen.client.util;

import dev.amble.lib.client.bedrock.BedrockAnimation;
import dev.amble.timelordregen.core.particle_effects.RegenParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * High-level API for spawning particles at animated bone positions.
 */
@Environment(EnvType.CLIENT)
public class BoneParticleSpawner {

    /**
     * Spawns a particle at a bone position using the ModelPart's current posed state.
     * The ModelPart should already have animation transforms applied.
     *
     * @param world       The client world
     * @param entity      The living entity
     * @param part        The ModelPart (already posed by animation system)
     * @param spawnPoint  The bone spawn point configuration
     * @param speed       Particle speed
     */
    public static void spawnAtBone(
            ClientWorld world,
            LivingEntity entity,
            ModelPart part,
            BoneSpawnPoint spawnPoint,
            float speed
    ) {
        if (part == null) return;

        BoneTransformResolver.BoneWorldTransform transform = BoneTransformResolver.resolveFromPose(
                entity,
                part,
                spawnPoint.localOffset(),
                spawnPoint.baseDirection(),
                entity.bodyYaw
        );

        world.addParticle(
                new RegenParticleEffect(
                        entity.getId(),
                        transform.yawDegrees(),
                        transform.pitchDegrees() - 90,
                        true,   // shouldPitch
                        false,  // shouldFollowPlayer - we're providing absolute angles
                        speed
                ),
                transform.position().x,
                transform.position().y,
                transform.position().z,
                0, 0, 0
        );
    }

    /**
     * Determines particle speed based on animation stage.
     *
     * @param animation The current animation (nullable)
     * @return Particle speed value
     */
    public static float getSpeedForAnimation(@Nullable BedrockAnimation animation) {
        if (animation == null) return 0.4f;

        String animName = animation.name.toLowerCase();
        if (animName.contains("loop")) {
            return 0.4f;  // Full intensity during loop
        } else if (animName.contains("end")) {
            return 0f;    // No particles at end
        } else {
            // Start animation - building up
            return 0.25f;
        }
    }
}
