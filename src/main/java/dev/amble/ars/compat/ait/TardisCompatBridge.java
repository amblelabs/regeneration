package dev.amble.ars.compat.ait;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public interface TardisCompatBridge {
    void tickRegenerationOverload(LivingEntity entity, Vec3d center);
}