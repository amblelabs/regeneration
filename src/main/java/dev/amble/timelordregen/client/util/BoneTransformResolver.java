package dev.amble.timelordregen.client.util;

import dev.amble.lib.client.bedrock.BedrockAnimation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Resolves world-space position and direction from animated bone data.
 *
 * Transform chain:
 * 1. Start with bone pivot point (from ModelPart current transform)
 * 2. Add local offset (configurable, e.g., hand tip offset) - rotated by bone rotation
 * 3. Apply entity body yaw rotation
 * 4. Scale from model units (1 unit = 1/16 block) to world units
 * 5. Add entity world position
 */
public class BoneTransformResolver {

    private static final double MODEL_UNIT_TO_WORLD = 1.0 / 16.0;

    /**
     * Result of bone transform resolution.
     */
    public record BoneWorldTransform(
        Vec3d position,      // World-space position
        Vec3d direction,     // Normalized direction vector in world space
        float yawDegrees,    // Direction as yaw angle (for particle)
        float pitchDegrees   // Direction as pitch angle (for particle)
    ) {}

    /**
     * Resolves the world-space transform for a bone using its current posed state.
     * This reads directly from the ModelPart which has already been posed by the animation system.
     *
     * @param entity        The living entity
     * @param part          The ModelPart for the bone (already posed)
     * @param localOffset   Offset from pivot in model units
     * @param baseDirection Base direction vector before rotation
     * @param bodyYaw       Entity body yaw in degrees
     * @return BoneWorldTransform with position and direction
     */
    public static BoneWorldTransform resolveFromPose(
            LivingEntity entity,
            ModelPart part,
            Vec3d localOffset,
            Vec3d baseDirection,
            float bodyYaw
    ) {
        // Step 1: Get bone pivot point (in model units)
        // The animation system sets these values on the ModelPart
        double pivotX = part.pivotX;
        double pivotY = part.pivotY;
        double pivotZ = part.pivotZ;

        // Step 2: Get current rotation (in radians, already includes animation)
        double pitch = part.pitch;
        double yaw = part.yaw;
        double roll = part.roll;

        // Step 3: Rotate local offset by bone rotation
        Vec3d rotatedOffset = rotateByEuler(localOffset, pitch, yaw, roll);

        // Step 4: Calculate position in model space
        // Position = pivot + rotated local offset
        double modelX = pivotX + rotatedOffset.x;
        double modelY = pivotY + rotatedOffset.y;
        double modelZ = pivotZ + rotatedOffset.z;

        // Step 5: Rotate by entity body yaw
        double bodyYawRad = Math.toRadians(-bodyYaw);
        double cosBody = Math.cos(bodyYawRad);
        double sinBody = Math.sin(bodyYawRad);

        double worldOffsetX = modelX * cosBody - modelZ * sinBody;
        double worldOffsetZ = modelX * sinBody + modelZ * cosBody;
        double worldOffsetY = -modelY; // Flip Y (model Y+ is down in Minecraft models)

        // Step 6: Scale to world units
        worldOffsetX *= MODEL_UNIT_TO_WORLD;
        worldOffsetY *= MODEL_UNIT_TO_WORLD;
        worldOffsetZ *= MODEL_UNIT_TO_WORLD;

        // Step 7: Add entity position
        Vec3d worldPos = new Vec3d(
            entity.getX() + worldOffsetX,
            entity.getY() + entity.getStandingEyeHeight() + worldOffsetY,
            entity.getZ() + worldOffsetZ
        );

        // Step 8: Calculate direction
        Vec3d rotatedDir = rotateByEuler(baseDirection, pitch, yaw, roll);

        // Rotate direction by body yaw
        double worldDirX = rotatedDir.x * cosBody - rotatedDir.z * sinBody;
        double worldDirZ = rotatedDir.x * sinBody + rotatedDir.z * cosBody;
        double worldDirY = -rotatedDir.y; // Flip Y

        Vec3d worldDir = new Vec3d(worldDirX, worldDirY, worldDirZ).normalize();

        // Step 9: Convert direction to yaw/pitch for particle
        float particleYaw = (float) Math.toDegrees(Math.atan2(-worldDir.x, worldDir.z));
        float particlePitch = (float) Math.toDegrees(Math.asin(worldDir.y));

        return new BoneWorldTransform(worldPos, worldDir, particleYaw, particlePitch);
    }

    /**
     * Legacy method that samples animation data directly.
     * Prefer resolveFromPose() when the ModelPart has already been posed.
     */
    public static BoneWorldTransform resolve(
            LivingEntity entity,
            ModelPart part,
            String boneName,
            BedrockAnimation animation,
            double animationTime,
            Vec3d localOffset,
            Vec3d baseDirection,
            float bodyYaw
    ) {
        // Step 1: Get bone pivot point from default transform (in model units)
        ModelTransform defaultTransform = part.getDefaultTransform();
        double pivotX = defaultTransform.pivotX;
        double pivotY = defaultTransform.pivotY;
        double pivotZ = defaultTransform.pivotZ;

        // Step 2: Get default rotation (in radians)
        double defaultPitch = defaultTransform.pitch;
        double defaultYaw = defaultTransform.yaw;
        double defaultRoll = defaultTransform.roll;

        // Step 3: Get animation transforms
        Vec3d animPosition = Vec3d.ZERO;
        double animPitch = 0, animYaw = 0, animRoll = 0;

        if (animation != null && animation.boneTimelines != null && animation.boneTimelines.containsKey(boneName)) {
            BedrockAnimation.BoneTimeline timeline = animation.boneTimelines.get(boneName);

            // Animation position offset (Y-axis inverted in Bedrock format)
            animPosition = timeline.position().resolve(animationTime);
            animPosition = new Vec3d(animPosition.x, -animPosition.y, animPosition.z);

            // Animation rotation (in degrees, convert to radians)
            Vec3d animRotDeg = timeline.rotation().resolve(animationTime);
            animPitch = Math.toRadians(animRotDeg.x);
            animYaw = Math.toRadians(animRotDeg.y);
            animRoll = Math.toRadians(animRotDeg.z);
        }

        // Step 4: Combine rotations (default + animation)
        double totalPitch = defaultPitch + animPitch;
        double totalYaw = defaultYaw + animYaw;
        double totalRoll = defaultRoll + animRoll;

        // Step 5: Rotate local offset by bone rotation
        Vec3d rotatedOffset = rotateByEuler(localOffset, totalPitch, totalYaw, totalRoll);

        // Step 6: Calculate position in model space
        // Position = pivot + animation offset + rotated local offset
        double modelX = pivotX + animPosition.x + rotatedOffset.x;
        double modelY = pivotY + animPosition.y + rotatedOffset.y;
        double modelZ = pivotZ + animPosition.z + rotatedOffset.z;

        // Step 7: Rotate by entity body yaw
        double bodyYawRad = Math.toRadians(-bodyYaw);
        double cosBody = Math.cos(bodyYawRad);
        double sinBody = Math.sin(bodyYawRad);

        double worldOffsetX = modelX * cosBody - modelZ * sinBody;
        double worldOffsetZ = modelX * sinBody + modelZ * cosBody;
        double worldOffsetY = -modelY; // Flip Y (model Y+ is down in Minecraft models)

        // Step 8: Scale to world units
        worldOffsetX *= MODEL_UNIT_TO_WORLD;
        worldOffsetY *= MODEL_UNIT_TO_WORLD;
        worldOffsetZ *= MODEL_UNIT_TO_WORLD;

        // Step 9: Add entity position
        Vec3d worldPos = new Vec3d(
            entity.getX() + worldOffsetX,
            entity.getY() + entity.getStandingEyeHeight() + worldOffsetY,
            entity.getZ() + worldOffsetZ
        );

        // Step 10: Calculate direction
        Vec3d rotatedDir = rotateByEuler(baseDirection, totalPitch, totalYaw, totalRoll);

        // Rotate direction by body yaw
        double worldDirX = rotatedDir.x * cosBody - rotatedDir.z * sinBody;
        double worldDirZ = rotatedDir.x * sinBody + rotatedDir.z * cosBody;
        double worldDirY = -rotatedDir.y; // Flip Y

        Vec3d worldDir = new Vec3d(worldDirX, worldDirY, worldDirZ).normalize();

        // Step 11: Convert direction to yaw/pitch for particle
        float particleYaw = (float) Math.toDegrees(Math.atan2(-worldDir.x, worldDir.z));
        float particlePitch = (float) Math.toDegrees(Math.asin(worldDir.y));

        return new BoneWorldTransform(worldPos, worldDir, particleYaw, particlePitch);
    }

    /**
     * Rotates a vector by Euler angles (radians) in ZXY order (Roll, Pitch, Yaw).
     * This matches Minecraft's model rotation convention.
     */
    private static Vec3d rotateByEuler(Vec3d vec, double pitch, double yaw, double roll) {
        // Apply rotations in order: Roll (Z) -> Pitch (X) -> Yaw (Y)
        Vec3d result = rotateAroundZ(vec, roll);
        result = rotateAroundX(result, pitch);
        result = rotateAroundY(result, yaw);
        return result;
    }

    private static Vec3d rotateAroundX(Vec3d vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3d(
            vec.x,
            vec.y * cos - vec.z * sin,
            vec.y * sin + vec.z * cos
        );
    }

    private static Vec3d rotateAroundY(Vec3d vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3d(
            vec.x * cos + vec.z * sin,
            vec.y,
            -vec.x * sin + vec.z * cos
        );
    }

    private static Vec3d rotateAroundZ(Vec3d vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3d(
            vec.x * cos - vec.y * sin,
            vec.x * sin + vec.y * cos,
            vec.z
        );
    }
}
