package dev.amble.ars.core;

import dev.amble.ars.compat.ait.AITCompat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class RegenerationExplosion {

    //范围
    private static final double RADIUS = 6.0;
    //伤害
    private static final float DAMAGE_PER_TICK = 5.0f;

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

            //推力
            target.addVelocity(push.x * 0.6, 0.1, push.z * 0.6);

            target.velocityModified = true;

            target.damage(source.getDamageSources().magic(), DAMAGE_PER_TICK);
        }

        // 每2秒触发控制台过载
        if (source.age % 40 == 0) {
            AITCompat.tickRegenerationOverload(source, center);
        }
    }
}