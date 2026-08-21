package dev.amble.timelordregen.client.util;

import dev.amble.timelordregen.core.particle_effects.RegenParticleEffect;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ClientParticleUtil {

    public static void spawnForPart(ClientWorld world, LivingEntity entity,
                                    MatrixStack baseStack, ModelPart part,
                                    String partName, float lerpedValue, boolean shortLife,
                                    boolean isDelay) {

        if ("head".equals(partName)) {
            spawnHeadParticles(world, entity, baseStack, part, lerpedValue, shortLife, isDelay);
            return;
        }

        // ===== 手臂 =====
        final Vec3d[] pivotWorld = {null};
        final Vec3d[] palmCorners = new Vec3d[4];
        final double[] bestDistSq = {-1.0};

        part.forEachCuboid(baseStack, (entry, path, index, cuboid) -> {
            if (pivotWorld[0] == null) {
                Vector4f p = new Vector4f(0, 0, 0, 1.0F);
                entry.getPositionMatrix().transform(p);
                pivotWorld[0] = new Vec3d(p.x, p.y, p.z);
            }

            checkFace(entry, cuboid, true, pivotWorld[0], palmCorners, bestDistSq);
            checkFace(entry, cuboid, false, pivotWorld[0], palmCorners, bestDistSq);
        });

        if (bestDistSq[0] < 0 || pivotWorld[0] == null) return;

        Vec3d palmCenter = palmCorners[0].add(palmCorners[1])
                .add(palmCorners[2]).add(palmCorners[3]).multiply(0.25);

        Vec3d dir = palmCenter.subtract(pivotWorld[0]);
        if (dir.lengthSquared() > 0.001) {
            dir = dir.normalize();
        } else {
            dir = new Vec3d(0, 1, 0);
        }

        if (isDelay) {
            // 延缓期：在手掌端面内随机生成，速度为0，严格框在手掌范围内
            int count = shortLife ? 6 : 3;
            for (int i = 0; i < count; i++) {
                double u = Math.random();
                double v = Math.random();
                Vec3d p01 = lerp(palmCorners[0], palmCorners[1], u);
                Vec3d p32 = lerp(palmCorners[3], palmCorners[2], u);
                Vec3d emitPos = lerp(p01, p32, v);
                emitPos = emitPos.add(dir.multiply(-0.06));

                world.addParticle(
                        new RegenParticleEffect(entity.getId(), 0, 0, true, false, lerpedValue, shortLife),
                        emitPos.x, emitPos.y, emitPos.z,
                        0.0, 0.0, 0.0
                );
            }
        } else {
            // 动画期：在手掌端面四边形内随机发射，带圆锥形扩散
            // === 数量翻倍，接近头部密度 ===
            int count = shortLife ? 14 : 8;

            // === 圆锥扩散参数：半角增大到 32°，总张角 64° ===
            double spreadAngle = Math.toRadians(32.0);

            for (int i = 0; i < count; i++) {
                double u = Math.random();
                double v = Math.random();
                Vec3d p01 = lerp(palmCorners[0], palmCorners[1], u);
                Vec3d p32 = lerp(palmCorners[3], palmCorners[2], u);
                Vec3d emitPos = lerp(p01, p32, v);
                emitPos = emitPos.add(dir.multiply(-0.08));

                // === 速度范围稍微提高，让扩散更有张力 ===
                double speed = 1.0 + Math.random() * 0.6;

                // ========== 圆锥形扩散核心代码 ==========
                Vec3d worldUp = Math.abs(dir.y) < 0.99 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
                Vec3d axisX = dir.crossProduct(worldUp).normalize();
                Vec3d axisY = dir.crossProduct(axisX).normalize();

                double theta = Math.random() * 2.0 * Math.PI;
                double phi = Math.random() * spreadAngle;

                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);
                double cosTheta = Math.cos(theta);
                double sinTheta = Math.sin(theta);

                Vec3d coneDir = dir.multiply(cosPhi)
                        .add(axisX.multiply(cosTheta * sinPhi))
                        .add(axisY.multiply(sinTheta * sinPhi));

                double vx = coneDir.x * speed;
                double vy = coneDir.y * speed;
                double vz = coneDir.z * speed;
                // =======================================

                world.addParticle(
                        new RegenParticleEffect(entity.getId(), 0, 0, true, false, lerpedValue, shortLife),
                        emitPos.x, emitPos.y, emitPos.z,
                        vx, vy, vz
                );
            }
        }
    }

    private static void checkFace(MatrixStack.Entry entry, ModelPart.Cuboid cuboid, boolean minY,
                                  Vec3d pivotWorld, Vec3d[] outCorners, double[] bestDistSq) {
        float y = minY ? cuboid.minY : cuboid.maxY;
        float x1 = cuboid.minX / 16.0f, x2 = cuboid.maxX / 16.0f;
        float z1 = cuboid.minZ / 16.0f, z2 = cuboid.maxZ / 16.0f;
        float yb = y / 16.0f;

        Vector4f[] vs = {
                new Vector4f(x1, yb, z1, 1.0F), new Vector4f(x2, yb, z1, 1.0F),
                new Vector4f(x2, yb, z2, 1.0F), new Vector4f(x1, yb, z2, 1.0F)
        };
        Vec3d[] corners = new Vec3d[4];
        double avgDistSq = 0;
        for (int i = 0; i < 4; i++) {
            entry.getPositionMatrix().transform(vs[i]);
            corners[i] = new Vec3d(vs[i].x, vs[i].y, vs[i].z);
            avgDistSq += corners[i].squaredDistanceTo(pivotWorld);
        }
        avgDistSq *= 0.25;

        if (avgDistSq > bestDistSq[0]) {
            bestDistSq[0] = avgDistSq;
            System.arraycopy(corners, 0, outCorners, 0, 4);
        }
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        return a.multiply(1.0 - t).add(b.multiply(t));
    }

    private static void spawnHeadParticles(ClientWorld world, LivingEntity entity,
                                           MatrixStack baseStack, ModelPart part,
                                           float lerpedValue, boolean shortLife, boolean isDelay) {

        final float[] minX = {Float.MAX_VALUE};
        final float[] maxX = {-Float.MAX_VALUE};
        final float[] minZ = {Float.MAX_VALUE};
        final float[] maxZ = {-Float.MAX_VALUE};
        final float[] neckY = {Float.MAX_VALUE};
        final boolean[] has = {false};

        part.forEachCuboid(baseStack, (entry, path, index, cuboid) -> {
            float localCX = (cuboid.minX + cuboid.maxX) * 0.5f / 16.0f;
            float localNeckY = cuboid.maxY / 16.0f;
            float localCZ = (cuboid.minZ + cuboid.maxZ) * 0.5f / 16.0f;

            Vector4f v = new Vector4f(localCX, localNeckY, localCZ, 1.0F);
            entry.getPositionMatrix().transform(v);

            if (v.x < minX[0]) minX[0] = v.x;
            if (v.x > maxX[0]) maxX[0] = v.x;
            if (v.z < minZ[0]) minZ[0] = v.z;
            if (v.z > maxZ[0]) maxZ[0] = v.z;
            if (v.y < neckY[0]) neckY[0] = v.y;
            has[0] = true;
        });

        if (!has[0]) return;

        baseStack.push();
        baseStack.translate(part.pivotX / 16.0F, part.pivotY / 16.0F, part.pivotZ / 16.0F);
        if (part.roll != 0.0F) {
            baseStack.multiply(RotationAxis.POSITIVE_Z.rotation(part.roll));
        }
        if (part.yaw != 0.0F) {
            baseStack.multiply(RotationAxis.NEGATIVE_Y.rotation(part.yaw));
        }
        if (part.pitch != 0.0F) {
            baseStack.multiply(RotationAxis.POSITIVE_X.rotation(part.pitch));
        }

        Vector3f upDir = new Vector3f(0, -1, 0);
        baseStack.peek().getNormalMatrix().transform(upDir);
        if (upDir.length() > 0.001f) upDir.normalize();
        baseStack.pop();

        double expand = 0.35;
        double centerX = (minX[0] + maxX[0]) * 0.5;
        double centerZ = (minZ[0] + maxZ[0]) * 0.5;
        double rangeX = (maxX[0] - minX[0]) * 0.5 + expand;
        double rangeZ = (maxZ[0] - minZ[0]) * 0.5 + expand;

        int count = shortLife ? 12 : 8;
        for (int i = 0; i < count; i++) {
            double rx = centerX + (Math.random() - 0.5) * 2 * rangeX;
            double rz = centerZ + (Math.random() - 0.5) * 2 * rangeZ;
            Vec3d emitPos = new Vec3d(rx, neckY[0], rz);

            double vx, vy, vz;
            if (isDelay) {
                vx = (Math.random() - 0.5) * 0.02;
                vy = (Math.random() - 0.5) * 0.02;
                vz = (Math.random() - 0.5) * 0.02;
            } else {
                Vector3f dir = new Vector3f(upDir);
                dir.add((float) (Math.random() - 0.5) * 1.2f,
                        0.3f + (float) Math.random() * 0.5f,
                        (float) (Math.random() - 0.5) * 1.2f);
                dir.normalize();

                double speed = 0.3 + Math.random() * 0.3;
                vx = dir.x * speed + (Math.random() - 0.5) * 0.05;
                vy = dir.y * speed + Math.random() * 0.08;
                vz = dir.z * speed + (Math.random() - 0.5) * 0.05;
            }

            world.addParticle(
                    new RegenParticleEffect(entity.getId(), 0, 0, true, false, lerpedValue, shortLife),
                    emitPos.x, emitPos.y, emitPos.z,
                    vx, vy, vz
            );
        }
    }
}