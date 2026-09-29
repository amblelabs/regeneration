package dev.amble.timelordregen.client.energy;

import dev.amble.timelordregen.RegenerationMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class EnergyMotes {
    private static final int MAX = 14000;
    private static final int BATCH = 4000;
    private static final int FRAMES = 7;
    private static final RenderLayer SOFT = RenderLayer.getEntityTranslucentEmissive(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
    private static final RenderLayer HOT = RenderLayer.getEyes(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
    private static final Identifier[] IDS = new Identifier[FRAMES];

    private static final double[] X = new double[MAX];
    private static final double[] Y = new double[MAX];
    private static final double[] Z = new double[MAX];
    private static final double[] PX = new double[MAX];
    private static final double[] PY = new double[MAX];
    private static final double[] PZ = new double[MAX];
    private static final float[] VX = new float[MAX];
    private static final float[] VY = new float[MAX];
    private static final float[] VZ = new float[MAX];
    private static final float[] SIZE = new float[MAX];
    private static final float[] DRAG = new float[MAX];
    private static final float[] RISE = new float[MAX];
    private static final int[] AGE = new int[MAX];
    private static final int[] LIFE = new int[MAX];
    private static final int[] OFF = new int[MAX];
    private static final boolean[] GLOW = new boolean[MAX];
    private static final Sprite[] SPRITES = new Sprite[FRAMES];
    private static final Vector3f A = new Vector3f();
    private static final Vector3f B = new Vector3f();
    private static final Vector3f C = new Vector3f();
    private static final Vector3f D = new Vector3f();

    private static final int MAX_T = 64;
    private static final double[] TX = new double[MAX_T];
    private static final double[] TY = new double[MAX_T];
    private static final double[] TZ = new double[MAX_T];
    private static final float[] TS = new float[MAX_T];
    private static final float[] TA = new float[MAX_T];
    private static final int[] TF = new int[MAX_T];

    private static int n;
    private static int tn;

    static {
        for (int i = 0; i < FRAMES; i++) IDS[i] = RegenerationMod.id("regen_particle_" + i);
    }

    static void add(double x, double y, double z, double vx, double vy, double vz, float size, int life, float drag, float rise, boolean glow, Random r) {
        if (n >= MAX) return;
        int i = n++;
        X[i] = PX[i] = x;
        Y[i] = PY[i] = y;
        Z[i] = PZ[i] = z;
        VX[i] = (float) vx;
        VY[i] = (float) vy;
        VZ[i] = (float) vz;
        SIZE[i] = size;
        LIFE[i] = Math.max(2, life);
        AGE[i] = 0;
        DRAG[i] = drag;
        RISE[i] = rise;
        GLOW[i] = glow;
        OFF[i] = r.nextInt(FRAMES);
    }

    static void flash(double x, double y, double z, float size, int frame, float alpha) {
        if (tn >= MAX_T) return;
        TX[tn] = x;
        TY[tn] = y;
        TZ[tn] = z;
        TS[tn] = size;
        TF[tn] = frame;
        TA[tn++] = alpha;
    }

    static void tick() {
        int i = 0;
        while (i < n) {
            if (++AGE[i] >= LIFE[i]) {
                move(--n, i);
                continue;
            }
            PX[i] = X[i];
            PY[i] = Y[i];
            PZ[i] = Z[i];
            X[i] += VX[i];
            Y[i] += VY[i];
            Z[i] += VZ[i];
            VX[i] *= DRAG[i];
            VY[i] = VY[i] * DRAG[i] + RISE[i];
            VZ[i] *= DRAG[i];
            i++;
        }
    }

    static void clear() {
        n = 0;
    }

    private static void move(int from, int to) {
        if (from == to) return;
        X[to] = X[from];
        Y[to] = Y[from];
        Z[to] = Z[from];
        PX[to] = PX[from];
        PY[to] = PY[from];
        PZ[to] = PZ[from];
        VX[to] = VX[from];
        VY[to] = VY[from];
        VZ[to] = VZ[from];
        SIZE[to] = SIZE[from];
        DRAG[to] = DRAG[from];
        RISE[to] = RISE[from];
        AGE[to] = AGE[from];
        LIFE[to] = LIFE[from];
        OFF[to] = OFF[from];
        GLOW[to] = GLOW[from];
    }

    static void render(WorldRenderContext ctx) {
        if (n == 0 && tn == 0) return;
        VertexConsumerProvider vcp = ctx.consumers();
        if (!(vcp instanceof VertexConsumerProvider.Immediate imm)) {
            tn = 0;
            return;
        }
        if (!(MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE) instanceof SpriteAtlasTexture atlas)) return;
        for (int i = 0; i < FRAMES; i++) SPRITES[i] = atlas.getSprite(IDS[i]);

        Camera cam = ctx.camera();
        Quaternionf rot = cam.getRotation();
        A.set(-1.0f, -1.0f, 0.0f).rotate(rot);
        B.set(-1.0f, 1.0f, 0.0f).rotate(rot);
        C.set(1.0f, 1.0f, 0.0f).rotate(rot);
        D.set(1.0f, -1.0f, 0.0f).rotate(rot);
        Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
        pass(imm, SOFT, false, m, cam.getPos(), ctx.tickDelta());
        pass(imm, HOT, true, m, cam.getPos(), ctx.tickDelta());
        tn = 0;
    }

    private static void pass(VertexConsumerProvider.Immediate imm, RenderLayer layer, boolean glow, Matrix4f m, Vec3d c, float delta) {
        VertexConsumer vc = imm.getBuffer(layer);
        int batch = 0;
        for (int i = 0; i < tn && !glow; i++) {
            float x = (float) (TX[i] - c.x), y = (float) (TY[i] - c.y), z = (float) (TZ[i] - c.z), s = TS[i];
            Sprite sp = SPRITES[TF[i] % FRAMES];
            float u0 = sp.getMinU(), u1 = sp.getMaxU(), v0 = sp.getMinV(), v1 = sp.getMaxV();
            vtx(vc, m, x + A.x * s, y + A.y * s, z + A.z * s, 1.0f, 0.9f, 0.9f, TA[i], u1, v1);
            vtx(vc, m, x + B.x * s, y + B.y * s, z + B.z * s, 1.0f, 0.9f, 0.9f, TA[i], u1, v0);
            vtx(vc, m, x + C.x * s, y + C.y * s, z + C.z * s, 1.0f, 0.9f, 0.9f, TA[i], u0, v0);
            vtx(vc, m, x + D.x * s, y + D.y * s, z + D.z * s, 1.0f, 0.9f, 0.9f, TA[i], u0, v1);
        }
        for (int i = 0; i < n; i++) {
            if (GLOW[i] != glow) continue;
            float k = (AGE[i] + delta) / LIFE[i];
            if (k >= 1.0f) continue;
            float x = (float) (MathHelper.lerp(delta, PX[i], X[i]) - c.x);
            float y = (float) (MathHelper.lerp(delta, PY[i], Y[i]) - c.y);
            float z = (float) (MathHelper.lerp(delta, PZ[i], Z[i]) - c.z);
            float d2 = x * x + y * y + z * z;
            if (d2 < 1.0f) continue;
            float s = SIZE[i] * (k < 0.1f ? 0.5f + 5.0f * k : 1.0f - 0.35f * (k - 0.1f));
            float f = 1.0f - k;
            if (d2 < 6.25f) f *= (MathHelper.sqrt(d2) - 1.0f) / 1.5f;
            float r, g, b, a;
            if (glow) {
                f *= f;
                r = f;
                g = f * 0.85f;
                b = f * 0.45f;
                a = 1.0f;
            } else {
                r = 1.0f;
                g = 0.9f;
                b = 0.9f;
                a = 0.72f * MathHelper.sqrt(f);
            }
            Sprite sp = SPRITES[(AGE[i] + OFF[i]) % FRAMES];
            float u0 = sp.getMinU(), u1 = sp.getMaxU(), v0 = sp.getMinV(), v1 = sp.getMaxV();
            vtx(vc, m, x + A.x * s, y + A.y * s, z + A.z * s, r, g, b, a, u1, v1);
            vtx(vc, m, x + B.x * s, y + B.y * s, z + B.z * s, r, g, b, a, u1, v0);
            vtx(vc, m, x + C.x * s, y + C.y * s, z + C.z * s, r, g, b, a, u0, v0);
            vtx(vc, m, x + D.x * s, y + D.y * s, z + D.z * s, r, g, b, a, u0, v1);
            if (++batch >= BATCH) {
                imm.draw(layer);
                vc = imm.getBuffer(layer);
                batch = 0;
            }
        }
        imm.draw(layer);
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a, float u, float v) {
        vc.vertex(m, x, y, z).color(r, g, b, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0.0f, 1.0f, 0.0f).next();
    }
}
