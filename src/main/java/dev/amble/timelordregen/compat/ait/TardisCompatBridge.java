package dev.amble.timelordregen.compat.ait;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface TardisCompatBridge {
    void tickRegenerationOverload(LivingEntity entity, Vec3d center);

    default boolean isTardis(World world) {
        return false;
    }

    default boolean drawFuel(World world, double amount) {
        return true;
    }
}