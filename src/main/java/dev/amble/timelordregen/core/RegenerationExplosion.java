package dev.amble.timelordregen.core;

import dev.amble.timelordregen.compat.ait.TardisCompatBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class RegenerationExplosion {

    private static final double RADIUS = 7.0;
    private static final float DAMAGE_PER_TICK = 3.0f;

    private static TardisCompatBridge tardisBridge = null;

    public static void setTardisBridge(TardisCompatBridge bridge) {
        tardisBridge = bridge;
    }

    public static void tick(LivingEntity source) {
        if (source.getWorld().isClient) return;

        World world = source.getWorld();
        Vec3d center = source.getPos();
        Box area = new Box(
                center.x - RADIUS, center.y - RADIUS, center.z - RADIUS,
                center.x + RADIUS, center.y + RADIUS, center.z + RADIUS
        );

        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != source && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3d push = target.getPos().subtract(center).normalize();
            target.addVelocity(push.x * 0.8, 0.4, push.z * 0.8);
            target.velocityModified = true;

            target.damage(source.getDamageSources().magic(), DAMAGE_PER_TICK);
        }

        if (source.age % 20 == 0 && tardisBridge != null) {
            tardisBridge.tickRegenerationOverload(source, center);
        }
    }
}