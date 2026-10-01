package dev.amble.timelordregen.client.energy;

import dev.amble.timelordregen.client.config.RegenerationClientConfig;
import dev.amble.timelordregen.core.energy.EnergyAbility;
import dev.amble.timelordregen.core.energy.EnergyFxType;
import dev.amble.timelordregen.core.particle_effects.RegenParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.Iterator;

@Environment(EnvType.CLIENT)
public final class EnergyFx {
    private static final RegenParticleEffect MOTE = new RegenParticleEffect(-1, 0, 0, false, false, 0.2f, true);
    private static final RegenParticleEffect LINGER = new RegenParticleEffect(-1, 0, 0, false, false, 0.2f, false);
    private static final DustColorTransitionParticleEffect ASH = new DustColorTransitionParticleEffect(new Vector3f(1.0f, 0.8f, 0.3f), new Vector3f(0.3f, 0.17f, 0.06f), 1.2f);
    private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
    private static final int BUDGET = 600, CEILING = 4600;
    private static int budget = BUDGET;
    private static long kick = Long.MIN_VALUE / 2, released = Long.MIN_VALUE / 2;
    private static double rx, ry, rz, e0x, e0y, e0z, e1x, e1y, e1z;

    public static void play(ClientWorld w, int type, int entityId, Vec3d pos, float a, float b) {
        Random r = w.random;
        float k = amount(pos);
        long now = w.getTime();
        switch (type) {
            case EnergyFxType.IMPACT -> impact(w, r, pos, a > 0.0f ? a : 2.5f, k, now);
            case EnergyFxType.DISSOLVE -> dissolve(w, r, pos, k, now);
            case EnergyFxType.DISINTEGRATE -> disintegrate(w, r, pos, a, b, k, now);
            case EnergyFxType.RELEASE -> release(w, r, entityId, pos, a > 0.0f ? a : 6.0f, k, now);
            case EnergyFxType.CRACK -> crack(w, r, w.getEntityById(entityId), pos, k, now);
            case EnergyFxType.TRANSFER -> transfer(w, r, pos, w.getEntityById(entityId), k);
            case EnergyFxType.CHAIN -> strike(w, r, pos, a > 0.0f ? a : 3.0f, k, now);
            default -> {}
        }
    }

    public static void tickChannel(ClientWorld w, PlayerEntity caster, EnergyClient.ChannelView v) {
        if (MinecraftClient.getInstance().isPaused() || !caster.isAlive()) return;
        Random r = w.random;
        float k = amount(caster.getPos());
        boolean fp = EnergyBeamRenderer.firstPerson(caster);
        float pt = w.getTime() - v.phaseTick;
        if (v.ability == EnergyAbility.BLAST && v.phase == EnergyFxType.CHARGE) {
            float c = MathHelper.clamp(pt / EnergyClient.chargeTicks, 0.0f, 1.0f);
            for (int s = 0; s < 2; s++) {
                Vec3d h = EnergyBeamRenderer.hand(caster, s == 0, pt, 1.0f);
                converge(r, h, count(r, (8.0f + 26.0f * c) * k * (fp ? 0.4f : 1.0f)), fp ? 0.5 : 1.4);
                if (fp) continue;
                gather(w, r, h, count(r, (1.0f + 3.0f * c) * k));
                for (int i = count(r, (0.5f + 2.0f * c) * k); i > 0; i--)
                    add(w, ParticleTypes.ELECTRIC_SPARK, h.x, h.y, h.z, r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
            }
        } else if (v.ability == EnergyAbility.BLAST && v.phase == EnergyFxType.STREAM) {
            BlockHitResult hit = EnergyBeamRenderer.aim(caster, 1.0f);
            Vec3d end = hit.getPos();
            for (int s = 0; s < 2; s++) {
                Vec3d h = EnergyBeamRenderer.hand(caster, s == 0, 99.0f, 1.0f);
                Vec3d d = end.subtract(h);
                double len = d.length();
                if (len > 0.01) {
                    Vec3d dir = d.multiply(1.0 / len);
                    core(r, h, dir, len, fp ? 0.8 : 0.0, count(r, 30.0f * k));
                    volume(r, h, dir, len, fp ? 0.12 : 0.02, count(r, 22.0f * k));
                    if (!fp) cone(r, h, dir, 0.55, 0.5, 1.1, count(r, 14.0f * k), 0.2f, 0.45f, 10, 16, 0.9f, 0.0f, 0.3f);
                    stream(w, r, caster.getId(), fp ? h.add(dir.multiply(Math.min(1.4, len * 0.5))) : h, dir, count(r, 12.0f * k), 0.16, 1.4, 2.4);
                    body(w, r, caster.getId(), h, dir, len, count(r, 8.0f * k), fp ? 0.12 : 0.03);
                }
                flow(w, r, h, end, count(r, 5.0f * k), fp ? 0.25 : 0.0, 0.14);
                for (int i = count(r, 1.5f * k); i > 0; i--) {
                    double u = 0.15 + r.nextDouble() * 0.8;
                    add(w, ParticleTypes.END_ROD, h.x + d.x * u, h.y + d.y * u, h.z + d.z * u, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08);
                }
                if (fp) {
                    for (int i = count(r, k); i > 0; i--) {
                        double u = 0.3 + r.nextDouble() * 0.6;
                        embers(w, r, h.x + d.x * u, h.y + d.y * u, h.z + d.z * u, 1, 0.1);
                    }
                    continue;
                }
                for (int i = count(r, 1.2f * k); i > 0; i--)
                    add(w, ParticleTypes.ELECTRIC_SPARK, h.x, h.y, h.z, r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
            }
            spray(r, hit, count(r, 22.0f * k));
            splash(w, r, hit, k);
            if (!fp) cone(r, caster.getEyePos().add(0.0, 0.25, 0.0), UP, 0.6, 0.25, 0.6, count(r, 10.0f * k), 0.25f, 0.55f, 14, 22, 0.93f, 0.0f, 0.2f);
        } else if (v.ability == EnergyAbility.TRANSFER) {
            Entity t = w.getEntityById(v.targetId);
            if (t == null || t == caster) return;
            Vec3d from = fp ? EnergyBeamRenderer.hand(caster, true, -1.0f, 1.0f) : EnergyBeamRenderer.chest(caster, 1.0f);
            Vec3d to = EnergyBeamRenderer.chest(t, 1.0f);
            flow(w, r, from, to, count(r, 3.0f * k), fp ? 0.2 : 0.0, 0.06);
            trail(r, from, to, count(r, 12.0f * k), 0.12, fp ? 0.15 : 0.0);
        }
    }

    static void tickChains(ClientWorld w) {
        long now = w.getTime();
        Iterator<EnergyBeamRenderer.Chain> it = EnergyBeamRenderer.CHAINS.iterator();
        while (it.hasNext()) {
            EnergyBeamRenderer.Chain c = it.next();
            long dt = now - c.born;
            if (dt < 0 || c.over(now)) {
                it.remove();
                continue;
            }
            if (dt % c.hop != 0 || c.dim != w.getRegistryKey()) continue;
            int k = (int) (dt / c.hop);
            if (k >= c.ids.length) continue;
            Random r = w.random;
            Vec3d b = c.at(w, k, 1.0f);
            float amt = amount(b);
            if (k > 0) {
                trail(r, c.at(w, k - 1, 1.0f), b, count(r, 55.0f * amt), 0.35, 0.0);
            } else if (w.getEntityById(c.caster) instanceof PlayerEntity p) {
                boolean fp = EnergyBeamRenderer.firstPerson(p);
                for (int s = 0; s < 2; s++)
                    trail(r, EnergyBeamRenderer.hand(p, s == 0, 99.0f, 1.0f), b, count(r, 40.0f * amt), 0.3, fp ? 0.3 : 0.0);
                if (!fp) cone(r, p.getEyePos().add(0.0, 0.25, 0.0), UP, 0.6, 0.3, 0.8, count(r, 30.0f * amt), 0.25f, 0.6f, 14, 24, 0.92f, 0.0f, 0.3f);
            }
        }
    }

    static void tickBudget() {
        int alive;
        try {
            alive = Integer.parseInt(MinecraftClient.getInstance().particleManager.getDebugString());
        } catch (NumberFormatException e) {
            alive = 0;
        }
        budget = Math.max(0, Math.min(BUDGET, CEILING - alive));
    }

    private static void add(ClientWorld w, ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
        if (budget <= 0) return;
        budget--;
        w.addParticle(fx, x, y, z, vx, vy, vz);
    }

    public static float shake(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.isPaused()) return 0.0f;
        float kk = kick(tickDelta);
        EnergyClient.ChannelView v = EnergyClient.local();
        float s = 0.75f * kk * kk;
        if (v != null && v.ability == EnergyAbility.BLAST) {
            float pt = mc.world.getTime() - v.phaseTick + tickDelta;
            if (v.phase == EnergyFxType.CHARGE) {
                s = 0.35f * MathHelper.clamp(pt / EnergyClient.chargeTicks, 0.0f, 1.0f) + 0.5f * MathHelper.clamp((pt - EnergyClient.chargeTicks + 2.0f) / 2.0f, 0.0f, 1.0f);
            } else if (v.phase == EnergyFxType.STREAM) {
                s += 0.12f + 0.06f * MathHelper.sin(pt * 0.5f);
            }
        }
        return MathHelper.clamp(s, 0.0f, 1.0f) * mc.options.getDistortionEffectScale().getValue().floatValue() * RegenerationClientConfig.get().shake;
    }

    static float kick(float tickDelta) {
        ClientWorld w = MinecraftClient.getInstance().world;
        float t = w == null ? -1.0f : w.getTime() - kick + tickDelta;
        return t < 0.0f ? 0.0f : MathHelper.clamp(1.0f - t / 8.0f, 0.0f, 1.0f);
    }

    static float glare(float tickDelta) {
        ClientWorld w = MinecraftClient.getInstance().world;
        float t = w == null ? 99.0f : w.getTime() - released + tickDelta;
        return t < 0.0f || t > 6.0f || !RegenerationClientConfig.get().glare ? 0.0f : 0.8f * (1.0f - t / 6.0f);
    }

    private static void impact(ClientWorld w, Random r, Vec3d p, float radius, float k, long now) {
        blast(r, p, count(r, 80.0f * k), 0.2, 0.9, 0.25f, 0.6f, 10, 20, 0.86f, 0.004f, 0.5f);
        blast(r, p, count(r, 40.0f * k), 0.05, 0.3, 0.35f, 0.8f, 16, 28, 0.9f, 0.006f, 0.1f);
        wave(r, p, count(r, 40.0f * k), radius / 9.0, 0.3f, 0.6f, 12, 18, 0.3f);
        burst(w, r, MOTE, p, count(r, 30.0f * k), 0.1, 0.55);
        burst(w, r, ParticleTypes.END_ROD, p, count(r, 12.0f * k), 0.04, 0.4);
        embers(w, r, p.x, p.y, p.z, count(r, 10.0f * k), 0.8);
        flash(w, p.x, p.y, p.z);
        for (int i = count(r, 1.5f * k); i > 0; i--)
            add(w, ParticleTypes.EXPLOSION, p.x + r.nextGaussian() * 0.9, p.y + r.nextGaussian() * 0.6, p.z + r.nextGaussian() * 0.9, 1.6, 0.0, 0.0);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.FLARE, p, 2.2f, 9, now);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.RING_FACING, p, radius * 1.4f, 9, now);
        near(p, 1600.0, now);
    }

    private static void strike(ClientWorld w, Random r, Vec3d p, float radius, float k, long now) {
        blast(r, p, count(r, 100.0f * k), 0.25, 1.1, 0.25f, 0.6f, 10, 20, 0.85f, 0.004f, 0.5f);
        blast(r, p, count(r, 60.0f * k), 0.05, 0.35, 0.35f, 0.85f, 16, 30, 0.9f, 0.008f, 0.1f);
        wave(r, p.add(0.0, -0.6, 0.0), count(r, 50.0f * k), radius / 7.0, 0.3f, 0.6f, 12, 20, 0.3f);
        cone(r, p, UP, 0.35, 0.3, 0.9, count(r, 25.0f * k), 0.25f, 0.55f, 14, 24, 0.93f, 0.0f, 0.4f);
        burst(w, r, ParticleTypes.END_ROD, p, count(r, 10.0f * k), 0.1, 0.5);
        embers(w, r, p.x, p.y, p.z, count(r, 8.0f * k), 0.6);
        flash(w, p.x, p.y, p.z);
        for (int i = count(r, 1.2f * k); i > 0; i--)
            add(w, ParticleTypes.EXPLOSION, p.x + r.nextGaussian() * 0.7, p.y + r.nextGaussian() * 0.5, p.z + r.nextGaussian() * 0.7, 1.4, 0.0, 0.0);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.FLARE, p, 2.6f, 10, now);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.RING, p.add(0.0, -0.8, 0.0), radius * 1.3f, 12, now);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.COLUMN, p.add(0.0, -0.8, 0.0), 5.0f, 9, now);
        near(p, 1024.0, now);
    }

    private static void dissolve(ClientWorld w, Random r, Vec3d p, float k, long now) {
        BlockPos pos = BlockPos.ofFloored(p);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (int i = count(r, 30.0f * k); i > 0; i--) {
            EnergyMotes.add(x + r.nextDouble(), y + r.nextDouble(), z + r.nextDouble(),
                    (r.nextDouble() - 0.5) * 0.04, 0.02 + r.nextDouble() * 0.08, (r.nextDouble() - 0.5) * 0.04,
                    0.2f + r.nextFloat() * 0.3f, 14 + r.nextInt(14), 0.95f, 0.004f, r.nextFloat() < 0.3f, r);
        }
        for (int i = count(r, 6.0f * k); i > 0; i--)
            add(w, MOTE, x + r.nextDouble(), y + r.nextDouble(), z + r.nextDouble(), (r.nextDouble() - 0.5) * 0.03, 0.02 + r.nextDouble() * 0.06, (r.nextDouble() - 0.5) * 0.03);
        Vec3d c = Vec3d.ofCenter(pos);
        embers(w, r, c.x, c.y, c.z, count(r, 2.0f * k), 0.35);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.FLARE, c, 0.9f, 7, now);
    }

    private static void disintegrate(ClientWorld w, Random r, Vec3d p, float width, float height, float k, long now) {
        float wd = Math.max(0.3f, width), ht = Math.max(0.3f, height);
        int n = count(r, MathHelper.clamp(40.0f + wd * wd * ht * 45.0f, 40.0f, 160.0f) * k);
        for (int i = n * 2; i > 0; i--) {
            double dx = (r.nextDouble() - 0.5) * wd, dy = (r.nextDouble() - 0.5) * ht, dz = (r.nextDouble() - 0.5) * wd;
            if (r.nextFloat() < 0.7f) {
                int face = r.nextInt(3);
                double sg = r.nextBoolean() ? 0.5 : -0.5;
                if (face == 0) dx = sg * wd;
                else if (face == 1) dy = sg * ht;
                else dz = sg * wd;
            }
            double up = 0.015 + 0.06 * (dy / ht + 0.5);
            EnergyMotes.add(p.x + dx, p.y + dy, p.z + dz, dx * 0.04 - dz * 0.06, up, dz * 0.04 + dx * 0.06,
                    0.18f + r.nextFloat() * 0.3f, 18 + r.nextInt(18), 0.96f, 0.003f, r.nextFloat() < 0.35f, r);
        }
        for (int i = n / 2; i > 0; i--) {
            double dx = (r.nextDouble() - 0.5) * wd, dy = (r.nextDouble() - 0.5) * ht, dz = (r.nextDouble() - 0.5) * wd;
            float f = r.nextFloat();
            add(w, f < 0.5f ? LINGER : f < 0.8f ? ASH : ParticleTypes.END_ROD, p.x + dx, p.y + dy, p.z + dz, dx * 0.04 - dz * 0.06, 0.02 + r.nextDouble() * 0.05, dz * 0.04 + dx * 0.06);
        }
        embers(w, r, p.x, p.y, p.z, count(r, 8.0f * k), wd * 0.5);
        flash(w, p.x, p.y, p.z);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.FLARE, p, Math.max(wd, ht) * 1.2f + 0.6f, 26, now);
    }

    private static void release(ClientWorld w, Random r, int id, Vec3d p, float radius, float k, long now) {
        PlayerEntity me = MinecraftClient.getInstance().player;
        if (me != null && me.getId() == id) released = now;
        wave(r, p.add(0.0, 0.2, 0.0), count(r, 240.0f * k), radius / 10.0, 0.3f, 0.7f, 14, 24, 0.3f);
        wave(r, p.add(0.0, 1.0, 0.0), count(r, 100.0f * k), radius / 16.0, 0.35f, 0.8f, 18, 28, 0.1f);
        cone(r, p.add(0.0, 0.5, 0.0), UP, 0.25, 0.4, 1.2, count(r, 90.0f * k), 0.25f, 0.6f, 16, 28, 0.94f, 0.0f, 0.4f);
        ring(w, r, ParticleTypes.END_ROD, p, count(r, 30.0f * k), radius / 9.0);
        ring(w, r, LINGER, p.add(0.0, 0.8, 0.0), count(r, 30.0f * k), radius / 16.0);
        flash(w, p.x, p.y + 1.0, p.z);
        flash(w, p.x, p.y + 3.0, p.z);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.RING, p.add(0.0, 0.15, 0.0), radius, 14, now);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.COLUMN, p, 9.0f, 11, now);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.FLARE, p.add(0.0, 1.2, 0.0), 2.6f, 9, now);
    }

    private static void crack(ClientWorld w, Random r, Entity e, Vec3d c, float k, long now) {
        cone(r, c, UP, 0.7, 0.15, 0.5, count(r, 120.0f * k), 0.2f, 0.5f, 16, 28, 0.93f, 0.0f, 0.3f);
        for (int i = count(r, 14.0f * k); i > 0; i--) {
            double th = r.nextDouble() * Math.PI * 2.0, sp = r.nextDouble() * 0.55, s = 0.12 + r.nextDouble() * 0.25;
            add(w, MOTE, c.x, c.y, c.z, Math.cos(th) * sp * s, s, Math.sin(th) * sp * s);
        }
        burst(w, r, ParticleTypes.END_ROD, c, count(r, 8.0f * k), 0.03, 0.15);
        if (e instanceof PlayerEntity pl && !EnergyBeamRenderer.firstPerson(pl)) {
            for (int s = 0; s < 2; s++)
                blast(r, EnergyBeamRenderer.hand(pl, s == 0, -1.0f, 1.0f), count(r, 30.0f * k), 0.05, 0.25, 0.2f, 0.45f, 12, 20, 0.9f, 0.0f, 0.3f);
        }
        flash(w, c.x, c.y, c.z);
        EnergyBeamRenderer.pulse(EnergyBeamRenderer.FLARE, c, 1.5f, 14, now);
    }

    private static void transfer(ClientWorld w, Random r, Vec3d from, Entity to, float k) {
        Vec3d b = to != null ? EnergyBeamRenderer.chest(to, 1.0f) : from;
        flow(w, r, from, b, count(r, 6.0f * k), 0.0, 0.1);
        trail(r, from, b, count(r, 30.0f * k), 0.15, 0.0);
        orbit(r, b, count(r, 20.0f * k));
    }

    private static void near(Vec3d p, double d2, long now) {
        PlayerEntity me = MinecraftClient.getInstance().player;
        if (me != null && me.squaredDistanceTo(p) < d2) kick = now;
    }

    private static void basis(double x, double y, double z) {
        double px, py, pz;
        if (Math.abs(y) < 0.99) {
            px = -z;
            py = 0.0;
            pz = x;
        } else {
            px = 0.0;
            py = z;
            pz = -y;
        }
        double l = Math.sqrt(px * px + py * py + pz * pz);
        e0x = px / l;
        e0y = py / l;
        e0z = pz / l;
        px = y * e0z - z * e0y;
        py = z * e0x - x * e0z;
        pz = x * e0y - y * e0x;
        l = Math.sqrt(px * px + py * py + pz * pz);
        e1x = px / l;
        e1y = py / l;
        e1z = pz / l;
    }

    private static void core(Random r, Vec3d h, Vec3d dir, double len, double skip, int n) {
        if (len - skip < 0.5) return;
        basis(dir.x, dir.y, dir.z);
        while (n-- > 0) {
            double sp = 1.1 + r.nextDouble() * 1.5, off = skip + r.nextDouble() * sp;
            double j = 0.3 * r.nextGaussian(), th = r.nextDouble() * Math.PI * 2.0;
            double c = Math.cos(th), s = Math.sin(th);
            double ox = (e0x * c + e1x * s) * j, oy = (e0y * c + e1y * s) * j, oz = (e0z * c + e1z * s) * j;
            int life = MathHelper.clamp((int) ((len - off) / sp) + 1, 1, 40);
            EnergyMotes.add(h.x + dir.x * off + ox, h.y + dir.y * off + oy, h.z + dir.z * off + oz,
                    dir.x * sp + ox * 0.08, dir.y * sp + oy * 0.08, dir.z * sp + oz * 0.08,
                    0.16f + r.nextFloat() * 0.26f, life, 1.0f, 0.0f, r.nextFloat() < 0.6f, r);
        }
    }

    private static void volume(Random r, Vec3d h, Vec3d dir, double len, double from, int n) {
        basis(dir.x, dir.y, dir.z);
        while (n-- > 0) {
            double u = from + r.nextDouble() * (1.0 - from), th = r.nextDouble() * Math.PI * 2.0;
            double rad = (0.2 + 1.9 * u) * Math.sqrt(r.nextDouble());
            double c = Math.cos(th) * rad, s = Math.sin(th) * rad;
            double ox = e0x * c + e1x * s, oy = e0y * c + e1y * s, oz = e0z * c + e1z * s;
            double sp = 0.1 + r.nextDouble() * 0.5;
            EnergyMotes.add(h.x + dir.x * u * len + ox, h.y + dir.y * u * len + oy, h.z + dir.z * u * len + oz,
                    dir.x * sp + ox * 0.05, dir.y * sp + oy * 0.05, dir.z * sp + oz * 0.05,
                    0.22f + r.nextFloat() * 0.5f, 8 + r.nextInt(9), 0.92f, 0.003f, r.nextFloat() < 0.25f, r);
        }
    }

    private static void cone(Random r, Vec3d at, Vec3d dir, double half, double min, double max, int n, float s0, float s1, int l0, int l1, float drag, float rise, float glow) {
        basis(dir.x, dir.y, dir.z);
        while (n-- > 0) {
            double th = r.nextDouble() * Math.PI * 2.0, ph = r.nextDouble() * half, sp = min + r.nextDouble() * (max - min);
            double c = Math.cos(ph), s = Math.sin(ph), ct = Math.cos(th), st = Math.sin(th);
            double vx = dir.x * c + (e0x * ct + e1x * st) * s;
            double vy = dir.y * c + (e0y * ct + e1y * st) * s;
            double vz = dir.z * c + (e0z * ct + e1z * st) * s;
            EnergyMotes.add(at.x, at.y, at.z, vx * sp, vy * sp, vz * sp, s0 + r.nextFloat() * (s1 - s0), l0 + r.nextInt(l1 - l0 + 1), drag, rise, r.nextFloat() < glow, r);
        }
    }

    private static void blast(Random r, Vec3d p, int n, double min, double max, float s0, float s1, int l0, int l1, float drag, float rise, float glow) {
        while (n-- > 0) {
            dir(r);
            double s = min + r.nextDouble() * (max - min);
            EnergyMotes.add(p.x, p.y, p.z, rx * s, ry * s, rz * s, s0 + r.nextFloat() * (s1 - s0), l0 + r.nextInt(l1 - l0 + 1), drag, rise, r.nextFloat() < glow, r);
        }
    }

    private static void wave(Random r, Vec3d p, int n, double speed, float s0, float s1, int l0, int l1, float glow) {
        for (int i = 0; i < n; i++) {
            double th = (i + r.nextDouble() * 0.6) * Math.PI * 2.0 / n, c = Math.cos(th), s = Math.sin(th), v = speed * (0.85 + r.nextDouble() * 0.3);
            EnergyMotes.add(p.x + c * 0.3, p.y + r.nextDouble() * 0.3, p.z + s * 0.3, c * v, r.nextDouble() * 0.03, s * v,
                    s0 + r.nextFloat() * (s1 - s0), l0 + r.nextInt(l1 - l0 + 1), 0.93f, 0.002f, r.nextFloat() < glow, r);
        }
    }

    private static void orbit(Random r, Vec3d c, int n) {
        while (n-- > 0) {
            double th = r.nextDouble() * Math.PI * 2.0, rad = 0.9 + r.nextDouble() * 0.6, y = (r.nextDouble() - 0.6) * 1.8;
            double x = Math.cos(th) * rad, z = Math.sin(th) * rad;
            EnergyMotes.add(c.x + x, c.y + y, c.z + z, -z * 0.12 - x * 0.03, 0.02 + r.nextDouble() * 0.04, x * 0.12 - z * 0.03,
                    0.18f + r.nextFloat() * 0.35f, 14 + r.nextInt(12), 0.94f, 0.002f, r.nextFloat() < 0.25f, r);
        }
    }

    private static void converge(Random r, Vec3d h, int n, double reach) {
        while (n-- > 0) {
            dir(r);
            double s = reach * (0.5 + r.nextDouble() * 0.5), dx = rx * s, dy = ry * s, dz = rz * s;
            EnergyMotes.add(h.x + dx, h.y + dy, h.z + dz, -dx / 8.0, -dy / 8.0, -dz / 8.0, 0.15f + r.nextFloat() * 0.25f, 8, 1.0f, 0.0f, r.nextFloat() < 0.5f, r);
        }
    }

    private static void trail(Random r, Vec3d a, Vec3d b, int n, double jitter, double from) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        double len = Math.max(0.01, Math.sqrt(dx * dx + dy * dy + dz * dz));
        while (n-- > 0) {
            double u = from + r.nextDouble() * (1.0 - from), j = jitter * Math.sin(Math.PI * u);
            EnergyMotes.add(a.x + dx * u + r.nextGaussian() * j, a.y + dy * u + r.nextGaussian() * j, a.z + dz * u + r.nextGaussian() * j,
                    dx / len * 0.06 + r.nextGaussian() * 0.03, dy / len * 0.06 + r.nextGaussian() * 0.03 + 0.01, dz / len * 0.06 + r.nextGaussian() * 0.03,
                    0.18f + r.nextFloat() * 0.35f, 8 + r.nextInt(12), 0.9f, 0.002f, r.nextFloat() < 0.5f, r);
        }
    }

    private static void spray(Random r, BlockHitResult hit, int n) {
        Vec3d p = hit.getPos();
        boolean solid = hit.getType() == HitResult.Type.BLOCK;
        Direction side = hit.getSide();
        double nx = solid ? side.getOffsetX() : 0.0, ny = solid ? side.getOffsetY() : 0.0, nz = solid ? side.getOffsetZ() : 0.0;
        while (n-- > 0) {
            dir(r);
            double dot = rx * nx + ry * ny + rz * nz, s = 0.15 + r.nextDouble() * 0.7;
            if (dot < 0.0) {
                rx -= nx * dot * 2.0;
                ry -= ny * dot * 2.0;
                rz -= nz * dot * 2.0;
            }
            EnergyMotes.add(p.x + nx * 0.1, p.y + ny * 0.1, p.z + nz * 0.1, rx * s, ry * s, rz * s,
                    0.25f + r.nextFloat() * 0.45f, 8 + r.nextInt(11), 0.87f, 0.006f, r.nextFloat() < 0.5f, r);
        }
    }

    private static void splash(ClientWorld w, Random r, BlockHitResult hit, float k) {
        Vec3d p = hit.getPos();
        boolean solid = hit.getType() == HitResult.Type.BLOCK;
        Direction side = hit.getSide();
        double nx = solid ? side.getOffsetX() : 0.0, ny = solid ? side.getOffsetY() : 0.0, nz = solid ? side.getOffsetZ() : 0.0;
        double x = p.x + nx * 0.08, y = p.y + ny * 0.08, z = p.z + nz * 0.08;
        for (int i = count(r, 6.0f * k); i > 0; i--) {
            dir(r);
            double dot = rx * nx + ry * ny + rz * nz, s = 0.1 + r.nextDouble() * 0.3;
            if (dot < 0.0) {
                rx -= nx * dot * 2.0;
                ry -= ny * dot * 2.0;
                rz -= nz * dot * 2.0;
            }
            add(w, MOTE, x, y, z, rx * s, ry * s, rz * s);
        }
        for (int i = count(r, 1.5f * k); i > 0; i--)
            add(w, ParticleTypes.END_ROD, x, y, z, r.nextGaussian() * 0.06, r.nextGaussian() * 0.06, r.nextGaussian() * 0.06);
        if (!solid) return;
        if (r.nextFloat() < 0.5f * k) add(w, ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
        BlockState state = w.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        BlockStateParticleEffect crumble = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
        for (int i = count(r, 2.0f * k); i > 0; i--)
            add(w, crumble, x, y, z, nx * 0.2 + r.nextGaussian() * 0.1, ny * 0.2 + r.nextGaussian() * 0.1, nz * 0.2 + r.nextGaussian() * 0.1);
    }

    private static void body(ClientWorld w, Random r, int id, Vec3d h, Vec3d dir, double len, int n, double from) {
        if (n <= 0) return;
        RegenParticleEffect a = new RegenParticleEffect(id, 0, 0, true, false, 0.6f, true), b = new RegenParticleEffect(id, 0, 0, true, false, 0.6f, false);
        basis(dir.x, dir.y, dir.z);
        while (n-- > 0) {
            double u = from + r.nextDouble() * (1.0 - from), th = r.nextDouble() * Math.PI * 2.0;
            double rad = (0.15 + 1.1 * u) * Math.sqrt(r.nextDouble()), sp = 0.2 + r.nextDouble() * 0.7;
            double c = Math.cos(th) * rad, sn = Math.sin(th) * rad;
            double ox = e0x * c + e1x * sn, oy = e0y * c + e1y * sn, oz = e0z * c + e1z * sn;
            add(w, r.nextBoolean() ? a : b, h.x + dir.x * u * len + ox, h.y + dir.y * u * len + oy, h.z + dir.z * u * len + oz,
                    dir.x * sp + ox * 0.03, dir.y * sp + oy * 0.03, dir.z * sp + oz * 0.03);
        }
    }

    private static void stream(ClientWorld w, Random r, int id, Vec3d at, Vec3d dir, int n, double spread, double min, double max) {
        if (n <= 0) return;
        RegenParticleEffect a = new RegenParticleEffect(id, 0, 0, true, false, 0.6f, true), b = new RegenParticleEffect(id, 0, 0, true, false, 0.6f, false);
        basis(dir.x, dir.y, dir.z);
        while (n-- > 0) {
            double th = r.nextDouble() * Math.PI * 2.0, ph = r.nextDouble() * spread, sp = min + r.nextDouble() * (max - min);
            double c = Math.cos(ph), ct = Math.cos(th) * Math.sin(ph), st = Math.sin(th) * Math.sin(ph);
            add(w, r.nextBoolean() ? a : b, at.x, at.y, at.z, (dir.x * c + e0x * ct + e1x * st) * sp, (dir.y * c + e0y * ct + e1y * st) * sp, (dir.z * c + e0z * ct + e1z * st) * sp);
        }
    }

    private static void flow(ClientWorld w, Random r, Vec3d a, Vec3d b, int n, double from, double jitter) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        while (n-- > 0) {
            double u = from + r.nextDouble() * (0.9 - from), s = (1.0 - u) / 15.0, j = jitter * Math.sin(Math.PI * u);
            add(w, MOTE, a.x + dx * u + r.nextGaussian() * j, a.y + dy * u + r.nextGaussian() * j, a.z + dz * u + r.nextGaussian() * j, dx * s, dy * s, dz * s);
        }
    }

    private static void gather(ClientWorld w, Random r, Vec3d h, int n) {
        while (n-- > 0) {
            dir(r);
            double s = 0.7 + r.nextDouble() * 0.6, dx = rx * s, dy = ry * s, dz = rz * s;
            add(w, MOTE, h.x + dx, h.y + dy, h.z + dz, -dx / 15.0, -dy / 15.0, -dz / 15.0);
        }
    }

    private static void burst(ClientWorld w, Random r, ParticleEffect fx, Vec3d p, int n, double min, double max) {
        while (n-- > 0) {
            dir(r);
            double s = min + r.nextDouble() * (max - min);
            add(w, fx, p.x, p.y, p.z, rx * s, ry * s, rz * s);
        }
    }

    private static void ring(ClientWorld w, Random r, ParticleEffect fx, Vec3d p, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double th = (i + r.nextDouble() * 0.6) * Math.PI * 2.0 / n, c = Math.cos(th), s = Math.sin(th), v = speed * (0.85 + r.nextDouble() * 0.3);
            add(w, fx, p.x + c * 0.3, p.y + 0.1, p.z + s * 0.3, c * v, r.nextDouble() * 0.02, s * v);
        }
    }

    private static void embers(ClientWorld w, Random r, double x, double y, double z, int n, double spread) {
        while (n-- > 0)
            add(w, ParticleTypes.WAX_ON, x + r.nextGaussian() * spread, y + r.nextGaussian() * spread, z + r.nextGaussian() * spread, r.nextGaussian() * 2.0, 1.0 + r.nextDouble() * 3.0, r.nextGaussian() * 2.0);
    }

    private static void flash(ClientWorld w, double x, double y, double z) {
        if (MinecraftClient.getInstance().gameRenderer.getCamera().getPos().squaredDistanceTo(x, y, z) > 6.25)
            add(w, ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);
    }

    private static void dir(Random r) {
        double y = r.nextDouble() * 2.0 - 1.0, th = r.nextDouble() * Math.PI * 2.0, q = Math.sqrt(1.0 - y * y);
        rx = Math.cos(th) * q;
        ry = y;
        rz = Math.sin(th) * q;
    }

    private static int count(Random r, float n) {
        int c = (int) n;
        return r.nextFloat() < n - c ? c + 1 : c;
    }

    private static float amount(Vec3d pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float k = RegenerationClientConfig.get().particles * switch (mc.options.getParticles().getValue()) {
            case ALL -> 1.0f;
            case DECREASED -> 0.45f;
            case MINIMAL -> 0.15f;
        };
        double d = mc.gameRenderer.getCamera().getPos().squaredDistanceTo(pos);
        return d > 4096.0 ? k * 0.3f : d > 1024.0 ? k * 0.6f : k;
    }
}
