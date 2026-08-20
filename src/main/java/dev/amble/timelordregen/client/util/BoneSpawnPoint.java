package dev.amble.timelordregen.client.util;

import net.minecraft.util.math.Vec3d;

/**
 * Defines a particle spawn point relative to a bone.
 *
 * @param boneName      Name of the bone in the model/animation
 * @param localOffset   Offset from pivot in model units (1 unit = 1/16 block)
 * @param baseDirection Base direction vector for particle velocity (before rotation)
 */
public record BoneSpawnPoint(
    String boneName,
    Vec3d localOffset,
    Vec3d baseDirection
) {
    // Predefined spawn points for regeneration effect

    /** Right arm - spawns at hand, particles shoot outward along arm */
    public static final BoneSpawnPoint RIGHT_ARM = new BoneSpawnPoint(
        "right_arm",
        new Vec3d(0, 10, 0),   // 10 units down from shoulder pivot toward hand
        new Vec3d(0, 1, 0)    // Direction: down the arm
    );

    /** Left arm - spawns at hand, particles shoot outward along arm */
    public static final BoneSpawnPoint LEFT_ARM = new BoneSpawnPoint(
        "left_arm",
        new Vec3d(0, 10, 0),   // 10 units down from shoulder pivot toward hand
        new Vec3d(0, 1, 0)    // Direction: down the arm
    );

    /** Head - spawns at face, particles shoot forward */
    public static final BoneSpawnPoint HEAD = new BoneSpawnPoint(
        "head",
        new Vec3d(0, -4, -4),  // Forward and slightly down from head pivot
        new Vec3d(0, 0, -1)   // Direction: forward from face
    );

    /** Creates a spawn point at the bone's pivot with no offset */
    public static BoneSpawnPoint atPivot(String boneName, Vec3d direction) {
        return new BoneSpawnPoint(boneName, Vec3d.ZERO, direction);
    }

    /** Creates a spawn point with custom offset and direction */
    public static BoneSpawnPoint withOffset(String boneName, Vec3d offset, Vec3d direction) {
        return new BoneSpawnPoint(boneName, offset, direction);
    }
}
