package dev.amble.timelordregen.client.energy;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.amble.timelordregen.client.config.RegenerationClientConfig;
import dev.amble.timelordregen.core.energy.EnergyAbility;
import dev.amble.timelordregen.core.energy.EnergyFxType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class EnergyBeamRenderer {
    static final int RING = 1;
    static final int FLARE = 2;
    static final int COLUMN = 3;
    static final int RING_FACING = 4;
    static final int ARC_LIFE = 16;

    private static final RenderLayer SWIRL_BASE = RenderLayer.getEnergySwirl(new Identifier("textures/entity/guardian_beam.png"), 0.0f, 0.0f);
    private static final RenderLayer SWIRL = new RenderLayer("timelordregen_swirl", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
            () -> { SWIRL_BASE.startDrawing(); RenderSystem.depthMask(false); }, () -> { RenderSystem.depthMask(true); SWIRL_BASE.endDrawing(); }) {};
    private static final RenderLayer GLOW = new RenderLayer("timelordregen_glow", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 256, false, true,
            () -> { RenderLayer.getLightning().startDrawing(); RenderSystem.depthMask(false); }, () -> { RenderSystem.depthMask(true); RenderLayer.getLightning().endDrawing(); }) {};
    private static final int SEG = 32;
    private static final int MAX_BEAMS = 16;
    private static final int STRIDE = 10;

    private static final float[] HOT = {1.0f, 0.97f, 0.86f};
    private static final float[] AMBER = {1.0f, 0.8f, 0.3f};
    private static final float[] GOLD = {1.0f, 0.62f, 0.12f};
    private static final float[] CX = {1.0f, -1.0f, -1.0f, 1.0f, 1.0f};
    private static final float[] CY = {1.0f, 1.0f, -1.0f, -1.0f, 1.0f};
    private static final int[] FACES = {0, 1, 3, 2, 4, 6, 7, 5, 0, 4, 5, 1, 2, 3, 7, 6, 0, 2, 6, 4, 1, 5, 7, 3};

    private static final float[] KT = {0.0f, 4.4f, 6.2f, 7.1f, 8.2f, 10.6f};
    private static final float[] KX = {0.35f, 0.06f, 0.07f, 0.44f, 0.88f, 0.92f};
    private static final float[] KY = {0.7f, 0.8f, 0.8f, 1.14f, 1.24f, 1.41f};
    private static final float[] KZ = {0.02f, 0.3f, 0.33f, 0.58f, -0.05f, -0.13f};

    private static final float[] X = new float[SEG + 1];
    private static final float[] Y = new float[SEG + 1];
    private static final float[] Z = new float[SEG + 1];
    private static final float[] U = new float[SEG + 1];
    private static final float[] D = new float[SEG + 1];
    private static final float[] I = new float[SEG + 1];
    private static final float[] QX = new float[8];
    private static final float[] QY = new float[8];
    private static final float[] QZ = new float[8];
    private static final float[] BEAMS = new float[MAX_BEAMS * STRIDE];
    private static final Pulse[] PULSES = new Pulse[32];
    static final List<Chain> CHAINS = new ArrayList<>();

    private static int beams;
    private static float jit;
    private static int seed;
    private static long quiet = Long.MIN_VALUE;
    private static float fovk = 1.0f;
    private static double cx, cy, cz;
    private static float rgtX, rgtY, rgtZ, upX, upY, upZ;
    private static float sx, sy, sz, ux, uy, uz;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EnergyBeamRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        ClientWorld w = ctx.world();
        VertexConsumerProvider vcp = ctx.consumers();
        if (w == null || vcp == null) return;
        Matrix4f pm = ctx.projectionMatrix();
        float py = MathHelper.sqrt(pm.m01() * pm.m01() + pm.m11() * pm.m11() + pm.m21() * pm.m21());
        if (py > 0.01f) fovk = MathHelper.clamp(1.428148f / py, 0.5f, 3.0f); // 1/tan(35deg), fp hand is drawn at fov 70
        EnergyClient.ChannelView lv = EnergyClient.local();
        PlayerEntity me = MinecraftClient.getInstance().player;
        if (lv != null && lv.ability == EnergyAbility.HEAL && me != null && me.getWorld() == w && firstPerson(me) && RegenerationClientConfig.get().handGlow) palms(me, me.age + ctx.tickDelta(), ctx.tickDelta());
        EnergyMotes.render(ctx);
        long now = w.getTime();
        if (EnergyClient.CHANNELS.isEmpty() && now > quiet) return;

        Camera cam = ctx.camera();
        Vec3d c = cam.getPos();
        Vector3f left = cam.getDiagonalPlane();
        Vector3f up = cam.getVerticalPlane();
        cx = c.x;
        cy = c.y;
        cz = c.z;
        rgtX = -left.x();
        rgtY = -left.y();
        rgtZ = -left.z();
        upX = up.x();
        upY = up.y();
        upZ = up.z();

        float delta = ctx.tickDelta();
        Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
        VertexConsumerProvider.Immediate imm = vcp instanceof VertexConsumerProvider.Immediate i ? i : null;
        beams = 0;
        for (Int2ObjectMap.Entry<EnergyClient.ChannelView> e : EnergyClient.CHANNELS.int2ObjectEntrySet()) {
            if (w.getEntityById(e.getIntKey()) instanceof PlayerEntity p && p.isAlive()) {
                channel(vcp.getBuffer(GLOW), m, ctx.frustum(), w, p, e.getValue(), now, delta);
                if (imm != null) imm.draw(GLOW);
            }
        }
        for (Chain ch : CHAINS) arcs(vcp, imm, m, w, ch, now, delta);
        pulses(vcp.getBuffer(GLOW), m, w, now, delta);
        if (imm != null) imm.draw(GLOW);

        for (int i = 0; i < beams; i++) {
            sheath(vcp.getBuffer(SWIRL), m, i);
            if (imm != null) imm.draw(SWIRL);
        }
    }

    private static void palms(PlayerEntity p, float t, float delta) {
        for (int s = 0; s < 2; s++) {
            Vec3d c = hand(p, s == 0, -1.0f, delta);
            for (int i = 0; i < 8; i++) {
                float at = t + h(i * 31 + s * 7) * 16.0f;
                int cyc = (int) (at / 16.0f);
                float age = at - cyc * 16.0f;
                int sd = i * 131 + s * 17 + cyc * 7919;
                EnergyMotes.flash(c.x + (h(sd) - 0.5f) * 0.1f, c.y + (h(sd + 1) - 0.5f) * 0.08f, c.z + (h(sd + 2) - 0.5f) * 0.1f,
                        (0.018f + 0.022f * h(sd + 3)) * fovk, (int) age + i, 0.7f - 0.025f * age);
            }
        }
    }

    private static void channel(VertexConsumer vc, Matrix4f m, Frustum fr, ClientWorld w, PlayerEntity p, EnergyClient.ChannelView v, long now, float delta) {
        float t = now - v.startTick + delta;
        float pt = now - v.phaseTick + delta;
        float ph = h(p.getId()) * MathHelper.TAU;
        boolean fp = firstPerson(p);
        if (v.ability == EnergyAbility.BLAST) {
            if (v.phase == EnergyFxType.CHARGE) {
                charge(vc, m, p, pt, now, delta, fp);
            } else if (v.phase == EnergyFxType.STREAM) {
                blast(vc, m, fr, p, t, pt, ph, delta, fp);
            }
        } else if (v.ability == EnergyAbility.TRANSFER) {
            tether(vc, m, fr, w, p, v.targetId, t, ph, delta, fp);
        }
    }

    private static void charge(VertexConsumer vc, Matrix4f m, PlayerEntity p, float pt, long now, float delta, boolean fp) {
        float k = MathHelper.clamp(pt / EnergyClient.chargeTicks, 0.0f, 1.0f);
        float s = fp ? 0.45f : 1.0f;
        int sd = (int) (now >> 1) * 31 + p.getId() * 131;
        Vec3d a = hand(p, false, pt, delta);
        Vec3d b = hand(p, true, pt, delta);
        float ax = (float) (a.x - cx), ay = (float) (a.y - cy), az = (float) (a.z - cz);
        float bx = (float) (b.x - cx), by = (float) (b.y - cy), bz = (float) (b.z - cz);
        float reach = (0.3f + 0.6f * k) * s;
        int n = 3 + (int) (6.0f * k);
        sparks(vc, m, ax, ay, az, reach, n, sd, 0.5f + 0.5f * k, 0.05f * s);
        sparks(vc, m, bx, by, bz, reach, n, sd + 7, 0.5f + 0.5f * k, 0.05f * s);
        if (a.squaredDistanceTo(b) < 0.36) {
            bolt(vc, m, ax, ay, az, bx, by, bz, sd + 11, 0.9f, 0.05f * s);
            bolt(vc, m, bx, by, bz, ax, ay, az, sd + 13, 0.9f, 0.04f * s);
        }
    }

    private static void blast(VertexConsumer vc, Matrix4f m, Frustum fr, PlayerEntity p, float t, float pt, float ph, float delta, boolean fp) {
        BlockHitResult hit = aim(p, delta);
        Vec3d end = hit.getPos();
        Vec3d a = hand(p, false, 99.0f, delta);
        Vec3d b = hand(p, true, 99.0f, delta);
        if (fr != null && !fr.isVisible(new Box(a, end).union(new Box(b, end)).expand(2.5))) return;
        float ext = MathHelper.clamp(pt / 3.0f, 0.1f, 1.0f);
        float ex = (float) (end.x - cx), ey = (float) (end.y - cy), ez = (float) (end.z - cz);
        beam(vc, m, a, ex, ey, ez, ext, t, ph, fp);
        beam(vc, m, b, ex, ey, ez, ext, t, ph + 2.9f, fp);
        if (ext < 1.0f) return;
        boolean solid = hit.getType() == HitResult.Type.BLOCK;
        float size = (solid ? 1.7f : 1.1f) * (1.0f + 0.14f * MathHelper.sin(t * 0.9f) + 0.07f * MathHelper.sin(t * 2.7f + 1.0f));
        flare(vc, m, ex, ey, ez, size, 1.0f, t);
        sparks(vc, m, ex, ey, ez, size * 2.2f, 4, (int) t * 17 + p.getId(), 0.9f, 0.07f);
    }

    private static void beam(VertexConsumer vc, Matrix4f m, Vec3d hand, float ex, float ey, float ez, float ext, float t, float ph, boolean fp) {
        float ax = (float) (hand.x - cx), ay = (float) (hand.y - cy), az = (float) (hand.z - cz);
        int frame = (int) (t * 2.5f);
        int fs = frame * 7919 + (int) (ph * 1000.0f);
        float wob = MathHelper.sin(t * 1.7f + ph) * MathHelper.sin(t * 0.63f + ph * 1.3f);
        float surge = 0.75f + 0.35f * wob + 0.45f * h(fs + 17) * h(fs + 29);
        float wander = 0.7f * ext * surge;
        float bx = ax + (ex - ax) * ext + (h(fs + 1) - 0.5f) * wander;
        float by = ay + (ey - ay) * ext + (h(fs + 2) - 0.5f) * wander;
        float bz = az + (ez - az) * ext + (h(fs + 3) - 0.5f) * wander;
        float w = (fp ? 0.55f : 1.0f) * surge * RegenerationClientConfig.get().beamScale;
        float near = fp ? 0.5f : 1.0f;
        jit = 0.3f * surge;
        seed = fs;
        int n = path(ax, ay, az, bx, by, bz, t, ph, 0.0f, 0.0f, 16);
        jit = 0.0f;
        if (n == 0) return;
        for (int i = 0; i <= n; i++) {
            I[i] *= 0.6f + 0.4f * h(fs + i * 13);
        }
        tube(vc, m, n, 1.6f * w * near, 4.4f * surge, GOLD, 0.12f);
        tube(vc, m, n, 1.0f * w * near, 2.8f * surge, GOLD, 0.18f);
        tube(vc, m, n, 0.6f * w * near, 1.6f * surge, AMBER, 0.28f);
        tube(vc, m, n, 0.28f * w * near, 0.7f * surge, HOT, 0.65f);
        float fw = Math.max(1.0f, MathHelper.sqrt(bx * bx + by * by + bz * bz) / 12.0f);
        for (int k = 0; k < 3; k++) {
            jit = 0.14f * surge;
            seed = fs + k * 101;
            path(ax, ay, az, bx, by, bz, t * (1.0f + k * 0.35f), ph + k * 2.1f, 1.2f * surge, k * MathHelper.TAU / 3.0f, 16);
            jit = 0.0f;
            tube(vc, m, n, 0.1f * w, 0.1f * fw * surge, AMBER, 0.6f);
        }
        branches(vc, m, ax, ay, az, bx, by, bz, fs, surge, w, 1.0f);
        if (ext < 1.0f) flare(vc, m, bx, by, bz, 0.7f, 1.0f, t);
        if (beams < MAX_BEAMS) {
            int o = beams++ * STRIDE;
            BEAMS[o] = ax;
            BEAMS[o + 1] = ay;
            BEAMS[o + 2] = az;
            BEAMS[o + 3] = bx;
            BEAMS[o + 4] = by;
            BEAMS[o + 5] = bz;
            BEAMS[o + 6] = t;
            BEAMS[o + 7] = ph;
            BEAMS[o + 8] = w;
            BEAMS[o + 9] = near;
        }
    }

    private static void branches(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, int fs, float surge, float w, float a) {
        int count = 2 + (int) (h(fs + 7) * 5.0f * surge);
        for (int i = 0; i < count; i++) {
            int s = fs * 7 + i * 977;
            float u = 0.12f + 0.85f * h(s);
            float px = ax + (bx - ax) * u, py = ay + (by - ay) * u, pz = az + (bz - az) * u;
            float len = (0.8f + 2.6f * h(s + 1)) * surge;
            float cy2 = h(s + 2) * 2.0f - 1.0f, th = h(s + 3) * MathHelper.TAU, q = MathHelper.sqrt(1.0f - cy2 * cy2);
            bolt(vc, m, px, py, pz, px + MathHelper.cos(th) * q * len, py + cy2 * len, pz + MathHelper.sin(th) * q * len, s + 5, (0.65f + 0.35f * h(s + 4)) * a, 0.06f * w + 0.03f);
        }
    }

    private static void arcs(VertexConsumerProvider vcp, VertexConsumerProvider.Immediate imm, Matrix4f m, ClientWorld w, Chain c, long now, float delta) {
        if (c.dim != w.getRegistryKey()) return;
        Entity src = w.getEntityById(c.caster);
        float scale = RegenerationClientConfig.get().beamScale;
        for (int k = 0; k < c.ids.length; k++) {
            float t = now - c.born - k * c.hop + delta;
            if (t < 0.0f) break;
            if (t > ARC_LIFE) continue;
            Vec3d b = c.at(w, k, delta);
            if (k > 0) {
                arc(vcp.getBuffer(GLOW), m, c.at(w, k - 1, delta), b, t, c.caster * 31 + k, scale, 1.0f);
            } else if (src instanceof PlayerEntity p) {
                float tp = firstPerson(p) ? 0.15f : 0.5f;
                arc(vcp.getBuffer(GLOW), m, hand(p, false, 99.0f, delta), b, t, c.caster * 31, scale, tp);
                if (imm != null) imm.draw(GLOW);
                arc(vcp.getBuffer(GLOW), m, hand(p, true, 99.0f, delta), b, t, c.caster * 31 + 977, scale, tp);
            }
            if (imm != null) imm.draw(GLOW);
        }
    }

    private static void arc(VertexConsumer vc, Matrix4f m, Vec3d from, Vec3d to, float t, int id, float scale, float tp) {
        float ax = (float) (from.x - cx), ay = (float) (from.y - cy), az = (float) (from.z - cz);
        float ex = (float) (to.x - cx), ey = (float) (to.y - cy), ez = (float) (to.z - cz);
        float ext = MathHelper.clamp(t / 1.5f, 0.05f, 1.0f);
        float a = t < 5.0f ? 1.0f : MathHelper.clamp(1.0f - (t - 5.0f) / (ARC_LIFE - 5.0f), 0.0f, 1.0f);
        a *= a;
        int fs = id * 7919 + (int) (t * 3.0f) * 104729;
        if (t > 5.0f && h(fs + 99) < 0.3f) a *= 0.25f;
        if (a < 0.01f) return;
        float bx = ax + (ex - ax) * ext, by = ay + (ey - ay) * ext, bz = az + (ez - az) * ext;
        float dx = ex - ax, dy = ey - ay, dz = ez - az;
        int seg = MathHelper.clamp((int) (MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 1.2f), 6, SEG);
        float ph = h(id) * MathHelper.TAU;
        float s = scale * (t < 3.0f ? 1.5f - t / 6.0f : 1.0f);
        jit = 0.45f;
        seed = fs;
        int n = path(ax, ay, az, bx, by, bz, t, ph, 0.0f, 0.0f, seg);
        jit = 0.0f;
        if (n == 0) return;
        for (int i = 0; i <= n; i++) {
            I[i] = a * (0.7f + 0.3f * h(fs + i * 13));
        }
        tube(vc, m, n, 2.4f * s * tp, 2.4f * s, GOLD, 0.14f);
        tube(vc, m, n, 1.5f * s * tp, 1.5f * s, GOLD, 0.2f);
        tube(vc, m, n, 0.85f * s * tp, 0.85f * s, AMBER, 0.35f);
        tube(vc, m, n, 0.35f * s * tp, 0.35f * s, HOT, 0.85f);
        for (int q = 1; q <= 2; q++) {
            jit = 0.75f;
            seed = fs + q * 31;
            path(ax, ay, az, bx, by, bz, t * (1.0f + q * 0.4f), ph + q * 2.3f, 0.0f, 0.0f, seg);
            jit = 0.0f;
            for (int i = 0; i <= n; i++) {
                I[i] *= a;
            }
            tube(vc, m, n, 0.12f * s * tp, 0.12f * s, HOT, 0.75f);
        }
        branches(vc, m, ax, ay, az, bx, by, bz, fs, 1.0f + a, s, a);
        if (ext >= 1.0f) flare(vc, m, bx, by, bz, (0.6f + 0.5f * a) * s, a, t * 4.0f);
    }

    private static void tether(VertexConsumer vc, Matrix4f m, Frustum fr, ClientWorld w, PlayerEntity p, int targetId, float t, float ph, float delta, boolean fp) {
        Entity e = w.getEntityById(targetId);
        if (e == null || e == p) return;
        Vec3d a = fp ? hand(p, true, -1.0f, delta) : chest(p, delta);
        Vec3d b = chest(e, delta);
        if (fr != null && !fr.isVisible(new Box(a, b).expand(1.0))) return;
        float ax = (float) (a.x - cx), ay = (float) (a.y - cy), az = (float) (a.z - cz);
        float bx = (float) (b.x - cx), by = (float) (b.y - cy), bz = (float) (b.z - cz);
        int n = path(ax, ay, az, bx, by, bz, t, ph, 0.0f, 0.0f, 16);
        if (n == 0) return;
        for (int i = 0; i <= n; i++) {
            I[i] = 0.55f + 0.45f * MathHelper.sin(D[i] * 2.2f - t * 1.6f + ph);
        }
        tube(vc, m, n, fp ? 0.1f : 0.3f, 0.3f, GOLD, 0.25f);
        tube(vc, m, n, fp ? 0.04f : 0.1f, 0.1f, HOT, 0.8f);
    }

    private static void pulses(VertexConsumer vc, Matrix4f m, ClientWorld w, long now, float delta) {
        if (now > quiet) return;
        boolean live = false;
        for (Pulse q : PULSES) {
            if (q == null || q.kind == 0) continue;
            float age = now - q.born + delta;
            if (age < 0.0f || age > q.life) {
                q.kind = 0;
                continue;
            }
            live = true;
            if (q.dim != w.getRegistryKey()) continue;
            float k = age / q.life, f = 1.0f - k;
            float x = (float) (q.x - cx), y = (float) (q.y - cy), z = (float) (q.z - cz);
            if (q.kind == RING) {
                ring(vc, m, x, y + 0.05f, z, q.size * (1.0f - f * f * f), 0.3f + 0.7f * k, f * f * 0.9f, false);
            } else if (q.kind == RING_FACING) {
                ring(vc, m, x, y, z, q.size * (1.0f - f * f * f), 0.3f + 0.7f * k, f * f * 0.9f, true);
            } else if (q.kind == FLARE) {
                flare(vc, m, x, y, z, q.size * (0.35f + 0.3f * MathHelper.sqrt(k)), f * f, age * 3.0f);
            } else {
                column(vc, m, x, y, z, q.size * Math.min(1.0f, k * 4.0f), 0.3f + 1.1f * f, f * f);
            }
        }
        if (!live) quiet = Long.MIN_VALUE;
    }

    private static int path(float ax, float ay, float az, float bx, float by, float bz, float t, float ph, float spiral, float spin, int seg) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return 0;
        basis(dx, dy, dz);
        float amp = MathHelper.clamp(0.06f + len * 0.02f, 0.06f, 0.5f);
        if (jit > 0.0f) amp *= 2.4f;
        float k1 = MathHelper.TAU * MathHelper.clamp(len * 0.125f, 0.5f, 2.0f);
        float k2 = MathHelper.TAU * MathHelper.clamp(len * 0.2f, 0.8f, 3.0f);
        float k3 = MathHelper.TAU * MathHelper.clamp(len * 0.2f, 1.0f, 3.0f);
        for (int i = 0; i <= seg; i++) {
            float u = i / (float) seg;
            float env = MathHelper.sin(u * MathHelper.PI) * (0.35f + 0.65f * u) * amp;
            float o1 = (MathHelper.sin(u * k1 - t * 0.6f + ph) + 0.4f * MathHelper.sin(u * k2 + t * 1.1f + ph * 1.7f)) * env;
            float o2 = (MathHelper.cos(u * k1 * 1.1f - t * 0.7f + ph * 0.6f) + 0.4f * MathHelper.sin(u * k2 * 0.9f - t * 1.3f + ph * 2.3f)) * env;
            if (jit > 0.0f && i > 0 && i < seg) {
                o1 += (h(seed + i * 7) - 0.5f) * jit * 2.0f;
                o2 += (h(seed + i * 7 + 3) - 0.5f) * jit * 2.0f;
            }
            if (spiral > 0.0f) {
                float sr = spiral * MathHelper.sqrt(Math.max(0.0f, MathHelper.sin(u * MathHelper.PI))) * (0.25f + 0.75f * u);
                float an = u * k3 - t * 0.55f + spin + ph;
                o1 += MathHelper.cos(an) * sr;
                o2 += MathHelper.sin(an) * sr;
            }
            X[i] = ax + dx * u + sx * o1 + ux * o2;
            Y[i] = ay + dy * u + sy * o1 + uy * o2;
            Z[i] = az + dz * u + sz * o1 + uz * o2;
            U[i] = u;
            D[i] = u * len;
            I[i] = 0.55f + 0.45f * MathHelper.sin(u * k3 - t * 1.6f + ph);
        }
        return seg;
    }

    private static void basis(float dx, float dy, float dz) {
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        float fx = dx / len, fy = dy / len, fz = dz / len;
        float px = -fz, py = 0.0f, pz = fx;
        if (Math.abs(fy) > 0.95f) {
            px = 0.0f;
            py = fz;
            pz = -fy;
        }
        float pl = MathHelper.sqrt(px * px + py * py + pz * pz);
        sx = px / pl;
        sy = py / pl;
        sz = pz / pl;
        ux = fy * sz - fz * sy;
        uy = fz * sx - fx * sz;
        uz = fx * sy - fy * sx;
    }

    private static void tube(VertexConsumer vc, Matrix4f m, int n, float w0, float w1, float[] c, float a) {
        for (int i = 0; i < n; i++) {
            int j = i + 1;
            float ri = (w0 + (w1 - w0) * U[i]) * 0.5f, rj = (w0 + (w1 - w0) * U[j]) * 0.5f;
            float ai = a * I[i], aj = a * I[j];
            for (int f = 0; f < 4; f++) {
                float ox0 = sx * CX[f] + ux * CY[f], oy0 = sy * CX[f] + uy * CY[f], oz0 = sz * CX[f] + uz * CY[f];
                float ox1 = sx * CX[f + 1] + ux * CY[f + 1], oy1 = sy * CX[f + 1] + uy * CY[f + 1], oz1 = sz * CX[f + 1] + uz * CY[f + 1];
                float x0 = X[i] + ox0 * ri, y0 = Y[i] + oy0 * ri, z0 = Z[i] + oz0 * ri;
                float x1 = X[j] + ox0 * rj, y1 = Y[j] + oy0 * rj, z1 = Z[j] + oz0 * rj;
                float x2 = X[j] + ox1 * rj, y2 = Y[j] + oy1 * rj, z2 = Z[j] + oz1 * rj;
                float x3 = X[i] + ox1 * ri, y3 = Y[i] + oy1 * ri, z3 = Z[i] + oz1 * ri;
                vtx(vc, m, x0, y0, z0, c, ai);
                vtx(vc, m, x1, y1, z1, c, aj);
                vtx(vc, m, x2, y2, z2, c, aj);
                vtx(vc, m, x3, y3, z3, c, ai);
                vtx(vc, m, x3, y3, z3, c, ai);
                vtx(vc, m, x2, y2, z2, c, aj);
                vtx(vc, m, x1, y1, z1, c, aj);
                vtx(vc, m, x0, y0, z0, c, ai);
            }
        }
    }

    private static void sheath(VertexConsumer vc, Matrix4f m, int b) {
        int o = b * STRIDE;
        float t = BEAMS[o + 6];
        int n = path(BEAMS[o], BEAMS[o + 1], BEAMS[o + 2], BEAMS[o + 3], BEAMS[o + 4], BEAMS[o + 5], t, BEAMS[o + 7], 0.0f, 0.0f, 16);
        if (n == 0) return;
        float w = BEAMS[o + 8], near = BEAMS[o + 9];
        swirl(vc, m, n, 2.0f * w * near, 5.0f * w, 0.35f, t * 0.22f, 0.7f);
        swirl(vc, m, n, 1.1f * w * near, 2.8f * w, 0.5f, t * 0.34f + 0.5f, 0.9f);
    }

    private static void swirl(VertexConsumer vc, Matrix4f m, int n, float w0, float w1, float scale, float scroll, float bright) {
        for (int i = 0; i < n; i++) {
            int j = i + 1;
            float ri = (w0 + (w1 - w0) * U[i]) * 0.5f, rj = (w0 + (w1 - w0) * U[j]) * 0.5f;
            float vi = D[i] * scale - scroll, vj = D[j] * scale - scroll;
            float ci = I[i] * bright, cj = I[j] * bright;
            for (int f = 0; f < 4; f++) {
                float ox0 = sx * CX[f] + ux * CY[f], oy0 = sy * CX[f] + uy * CY[f], oz0 = sz * CX[f] + uz * CY[f];
                float ox1 = sx * CX[f + 1] + ux * CY[f + 1], oy1 = sy * CX[f + 1] + uy * CY[f + 1], oz1 = sz * CX[f + 1] + uz * CY[f + 1];
                float x0 = X[i] + ox0 * ri, y0 = Y[i] + oy0 * ri, z0 = Z[i] + oz0 * ri;
                float x1 = X[j] + ox0 * rj, y1 = Y[j] + oy0 * rj, z1 = Z[j] + oz0 * rj;
                float x2 = X[j] + ox1 * rj, y2 = Y[j] + oy1 * rj, z2 = Z[j] + oz1 * rj;
                float x3 = X[i] + ox1 * ri, y3 = Y[i] + oy1 * ri, z3 = Z[i] + oz1 * ri;
                tex(vc, m, x0, y0, z0, 0.0f, vi, ci);
                tex(vc, m, x1, y1, z1, 0.0f, vj, cj);
                tex(vc, m, x2, y2, z2, 0.5f, vj, cj);
                tex(vc, m, x3, y3, z3, 0.5f, vi, ci);
                tex(vc, m, x3, y3, z3, 0.5f, vi, ci);
                tex(vc, m, x2, y2, z2, 0.5f, vj, cj);
                tex(vc, m, x1, y1, z1, 0.0f, vj, cj);
                tex(vc, m, x0, y0, z0, 0.0f, vi, ci);
            }
        }
    }

    private static void flare(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float a, float t) {
        if (x * x + y * y + z * z < r * r * 4.0f + 1.0f) return;
        cube(vc, m, x, y, z, r * 0.3f, t * 0.09f, HOT, a);
        cube(vc, m, x, y, z, r * 0.55f, -t * 0.06f + 0.4f, AMBER, 0.4f * a);
        cube(vc, m, x, y, z, r, t * 0.04f + 0.8f, GOLD, 0.16f * a);
    }

    private static void cube(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float spin, float[] c, float a) {
        float cs = MathHelper.cos(spin), sn = MathHelper.sin(spin);
        final float ct = 0.8164966f, st = 0.57735026f;
        for (int q = 0; q < 8; q++) {
            float i = (q & 1) != 0 ? r : -r, j = (q & 2) != 0 ? r : -r, k = (q & 4) != 0 ? r : -r;
            float x1 = i * cs + k * sn, z1 = k * cs - i * sn;
            QX[q] = x + x1;
            QY[q] = y + j * ct - z1 * st;
            QZ[q] = z + j * st + z1 * ct;
        }
        for (int f = 0; f < 24; f += 4) {
            int p0 = FACES[f], p1 = FACES[f + 1], p2 = FACES[f + 2], p3 = FACES[f + 3];
            vtx(vc, m, QX[p0], QY[p0], QZ[p0], c, a);
            vtx(vc, m, QX[p1], QY[p1], QZ[p1], c, a);
            vtx(vc, m, QX[p2], QY[p2], QZ[p2], c, a);
            vtx(vc, m, QX[p3], QY[p3], QZ[p3], c, a);
            vtx(vc, m, QX[p3], QY[p3], QZ[p3], c, a);
            vtx(vc, m, QX[p2], QY[p2], QZ[p2], c, a);
            vtx(vc, m, QX[p1], QY[p1], QZ[p1], c, a);
            vtx(vc, m, QX[p0], QY[p0], QZ[p0], c, a);
        }
    }

    private static void sparks(VertexConsumer vc, Matrix4f m, float x, float y, float z, float reach, int count, int seed, float alpha, float width) {
        for (int i = 0; i < count; i++) {
            int s = seed * 7919 + i * 104729;
            float cy = h(s) * 2.0f - 1.0f, th = h(s + 1) * MathHelper.TAU, q = MathHelper.sqrt(1.0f - cy * cy);
            float len = reach * (0.5f + 0.5f * h(s + 2));
            bolt(vc, m, x, y, z, x + MathHelper.cos(th) * q * len, y + cy * len, z + MathHelper.sin(th) * q * len, s + 5, alpha * (0.55f + 0.45f * h(s + 3)), width);
        }
    }

    private static void bolt(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, int seed, float alpha, float width) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;
        basis(dx, dy, dz);
        float jag = len * 0.3f;
        for (int i = 0; i <= 5; i++) {
            float f = i / 5.0f, k = i == 0 || i == 5 ? 0.0f : jag;
            X[i] = ax + dx * f + (h(seed + i * 3) - 0.5f) * k;
            Y[i] = ay + dy * f + (h(seed + i * 3 + 1) - 0.5f) * k;
            Z[i] = az + dz * f + (h(seed + i * 3 + 2) - 0.5f) * k;
            U[i] = f;
            I[i] = alpha;
        }
        tube(vc, m, 5, width * 3.0f, width * 1.2f, AMBER, 0.25f);
        tube(vc, m, 5, width, width * 0.4f, HOT, 1.0f);
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float th, float a, boolean facing) {
        band(vc, m, x, y, z, Math.max(0.0f, r - th * 0.5f), r + th * 0.5f, AMBER, a, facing);
        band(vc, m, x, y, z, r + th * 0.5f, r + th * 1.2f, GOLD, a * 0.35f, facing);
    }

    private static void band(VertexConsumer vc, Matrix4f m, float x, float y, float z, float in, float out, float[] c, float a, boolean facing) {
        float c0 = 1.0f, s0 = 0.0f;
        for (int i = 1; i <= 16; i++) {
            float an = i * MathHelper.TAU / 16.0f, c1 = MathHelper.cos(an), s1 = MathHelper.sin(an);
            float ax0, ay0, az0, ax1, ay1, az1;
            if (facing) {
                ax0 = rgtX * c0 + upX * s0;
                ay0 = rgtY * c0 + upY * s0;
                az0 = rgtZ * c0 + upZ * s0;
                ax1 = rgtX * c1 + upX * s1;
                ay1 = rgtY * c1 + upY * s1;
                az1 = rgtZ * c1 + upZ * s1;
            } else {
                ax0 = c0;
                ay0 = 0.0f;
                az0 = s0;
                ax1 = c1;
                ay1 = 0.0f;
                az1 = s1;
            }
            vtx(vc, m, x + ax0 * in, y + ay0 * in, z + az0 * in, c, a);
            vtx(vc, m, x + ax0 * out, y + ay0 * out, z + az0 * out, c, a);
            vtx(vc, m, x + ax1 * out, y + ay1 * out, z + az1 * out, c, a);
            vtx(vc, m, x + ax1 * in, y + ay1 * in, z + az1 * in, c, a);
            vtx(vc, m, x + ax1 * in, y + ay1 * in, z + az1 * in, c, a);
            vtx(vc, m, x + ax1 * out, y + ay1 * out, z + az1 * out, c, a);
            vtx(vc, m, x + ax0 * out, y + ay0 * out, z + az0 * out, c, a);
            vtx(vc, m, x + ax0 * in, y + ay0 * in, z + az0 * in, c, a);
            c0 = c1;
            s0 = s1;
        }
    }

    private static void column(VertexConsumer vc, Matrix4f m, float x, float y, float z, float height, float w, float a) {
        for (int i = 0; i <= 8; i++) {
            float f = i / 8.0f;
            X[i] = x;
            Y[i] = y + height * f;
            Z[i] = z;
            U[i] = f;
            I[i] = a * (1.0f - f * 0.8f);
        }
        sx = 1.0f;
        sy = 0.0f;
        sz = 0.0f;
        ux = 0.0f;
        uy = 0.0f;
        uz = 1.0f;
        tube(vc, m, 8, w, w * 0.3f, GOLD, 0.3f);
        tube(vc, m, 8, w * 0.5f, w * 0.15f, AMBER, 0.5f);
        tube(vc, m, 8, w * 0.2f, w * 0.06f, HOT, 1.0f);
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, float x, float y, float z, float[] c, float a) {
        vc.vertex(m, x, y, z).color(c[0], c[1], c[2], a).next();
    }

    private static void tex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, float c) {
        vc.vertex(m, x, y, z).color(c, c * 0.76f, c * 0.3f, 1.0f).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0.0f, 1.0f, 0.0f).next();
    }

    private static float h(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483648.0f;
    }

    static boolean firstPerson(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return e == mc.getCameraEntity() && mc.options.getPerspective().isFirstPerson();
    }

    static Vec3d chest(Entity e, float delta) {
        return e.getLerpedPos(delta).add(0.0, e.getHeight() * 0.62, 0.0);
    }

    static BlockHitResult aim(PlayerEntity p, float delta) {
        Vec3d eye = p.getCameraPosVec(delta);
        return p.getWorld().raycast(new RaycastContext(eye, eye.add(p.getRotationVec(delta).multiply(EnergyClient.range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
    }

    static Vec3d hand(PlayerEntity p, boolean right, float pose, float delta) {
        if (firstPerson(p)) {
            float x = 0.3f, y = -0.32f, z = 0.5f;
            if (pose >= 0.0f) {
                float k = MathHelper.clamp((pose - 5.0f) / 4.0f, 0.0f, 1.0f);
                x = MathHelper.lerp(k * k * (3.0f - 2.0f * k), 0.14f, 0.38f);
                y = -0.28f;
                z = 0.45f;
            }
            if (!right) x = -x;
            x *= fovk;
            y *= fovk;
            float yaw = p.getYaw(delta) * MathHelper.RADIANS_PER_DEGREE, pitch = p.getPitch(delta) * MathHelper.RADIANS_PER_DEGREE;
            float sy = MathHelper.sin(yaw), cyw = MathHelper.cos(yaw), sp = MathHelper.sin(pitch), cp = MathHelper.cos(pitch);
            Vec3d e = p.getCameraPosVec(delta);
            return new Vec3d(e.x - cyw * x - sp * sy * y - sy * cp * z, e.y + cp * y - sp * z, e.z - sy * x + sp * cyw * y + cyw * cp * z);
        }
        float x = 0.34f, y = 0.74f, z = 0.08f;
        if (pose >= 0.0f) {
            int i = 0;
            while (i < KT.length - 2 && pose > KT[i + 1]) i++;
            float k = MathHelper.clamp((pose - KT[i]) / (KT[i + 1] - KT[i]), 0.0f, 1.0f);
            x = MathHelper.lerp(k, KX[i], KX[i + 1]);
            y = MathHelper.lerp(k, KY[i], KY[i + 1]);
            z = MathHelper.lerp(k, KZ[i], KZ[i + 1]);
        }
        if (right) x = -x;
        if (p.isInSneakingPose()) y -= 0.28f;
        float b = MathHelper.lerpAngleDegrees(delta, p.prevBodyYaw, p.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        float c = MathHelper.cos(b), s = MathHelper.sin(b);
        Vec3d o = p.getLerpedPos(delta);
        return new Vec3d(o.x + x * c - z * s, o.y + y, o.z + x * s + z * c);
    }

    static void chain(int caster, int hop, int[] ids, double[] pos, long now) {
        CHAINS.add(new Chain(MinecraftClient.getInstance().world.getRegistryKey(), caster, hop, ids, pos, now));
        quiet = Math.max(quiet, now + (long) ids.length * hop + ARC_LIFE + 2);
    }

    static void pulse(int kind, Vec3d pos, float size, int life, long now) {
        int slot = 0;
        for (int i = 0; i < PULSES.length; i++) {
            Pulse q = PULSES[i];
            if (q == null || q.kind == 0 || now - q.born > q.life) {
                slot = i;
                break;
            }
            if (q.born < PULSES[slot].born) slot = i;
        }
        Pulse q = PULSES[slot];
        if (q == null) q = PULSES[slot] = new Pulse();
        q.kind = kind;
        q.x = pos.x;
        q.y = pos.y;
        q.z = pos.z;
        q.size = size;
        q.born = now;
        q.life = life;
        q.dim = MinecraftClient.getInstance().world.getRegistryKey();
        quiet = Math.max(quiet, now + life + 1);
    }

    static final class Chain {
        final RegistryKey<World> dim;
        final int caster;
        final int hop;
        final int[] ids;
        final double[] pos;
        final long born;

        Chain(RegistryKey<World> dim, int caster, int hop, int[] ids, double[] pos, long born) {
            this.dim = dim;
            this.caster = caster;
            this.hop = hop;
            this.ids = ids;
            this.pos = pos;
            this.born = born;
        }

        Vec3d at(ClientWorld w, int k, float delta) {
            Entity e = w.getEntityById(ids[k]);
            if (e != null && !e.isRemoved()) {
                Vec3d c = e.getLerpedPos(delta).add(0.0, e.getHeight() * 0.5, 0.0);
                pos[k * 3] = c.x;
                pos[k * 3 + 1] = c.y;
                pos[k * 3 + 2] = c.z;
                return c;
            }
            return new Vec3d(pos[k * 3], pos[k * 3 + 1], pos[k * 3 + 2]);
        }

        boolean over(long now) {
            return now - born > (long) ids.length * hop + ARC_LIFE + 1;
        }
    }

    private static final class Pulse {
        int kind;
        double x, y, z;
        float size;
        long born;
        int life;
        RegistryKey<World> dim;
    }
}
