package dev.amble.ars.client;

import dev.amble.lib.animation.AnimatedEntity;
import dev.amble.lib.client.bedrock.BedrockAnimation;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.client.util.ClientParticleUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ClientRegenParticleManager {

    public static void trySpawnForEntity(LivingEntity entity, float tickDelta) {
        if (!entity.isAlive()) return;
        if (!(entity instanceof RegenerationCapable capable)) return;
        capable.withInfo().ifPresent(info -> {
            if (!info.isActive()) return;
            if (!(entity instanceof AbstractClientPlayerEntity player)) return;

            boolean isDelay = info.getDelay().isRunning();
            float lerpedValue = resolveLerpedValue(entity);

            PlayerEntityRenderer renderer = (PlayerEntityRenderer) MinecraftClient.getInstance()
                    .getEntityRenderDispatcher().getRenderer(player);
            var model = renderer.getModel();
            ClientWorld world = (ClientWorld) entity.getWorld();

            MatrixStack baseStack = buildEntityWorldStack(entity);

            ClientParticleUtil.spawnForPart(world, entity, baseStack, model.rightArm, "right_arm", lerpedValue, false, isDelay);
            ClientParticleUtil.spawnForPart(world, entity, baseStack, model.leftArm, "left_arm", lerpedValue, false, isDelay);

            if (!isDelay) {
                ClientParticleUtil.spawnForPart(world, entity, baseStack, model.head, "head", lerpedValue, false, isDelay);
            }
        });
    }

    /**
     * 第一人称手臂粒子渲染 —— 已清理，不再生成粒子
     */
    public static void tryRenderFirstPersonArm(AbstractClientPlayerEntity player, ModelPart arm, String partName,
                                               MatrixStack matrices, VertexConsumerProvider provider, int light) {
        // 第一人称粒子系统已移除，如需恢复请参考历史版本
    }

    private static MatrixStack buildEntityWorldStack(LivingEntity entity) {
        MatrixStack stack = new MatrixStack();
        Vec3d pos = entity.getPos();
        stack.translate(pos.x, pos.y, pos.z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.bodyYaw));
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.5F, 0.0F);
        return stack;
    }

    private static float resolveLerpedValue(LivingEntity entity) {
        if (!(entity instanceof AnimatedEntity animated)) return 0.4f;

        BedrockAnimationReference ref = animated.getCurrentAnimation();
        if (ref == null) return 0.4f;

        BedrockAnimation anim = ref.get().orElse(null);
        if (anim == null) return 0.4f;

        AnimationState state = animated.getAnimationState();
        if (state == null) return 0.4f;

        double timeRunning = anim.getRunningSeconds(state);
        if (timeRunning >= anim.animationLength) return 0.4f;

        String animName = anim.name.toLowerCase();
        double animLength = anim.animationLength;
        if (animName.contains("loop")) {
            return 0.4f;
        } else if (animName.contains("end")) {
            return 0f;
        } else {
            float t = Math.max(0f, Math.min(1f, (float) (timeRunning / animLength)));
            return 0.01f + t * 0.3f;
        }
    }
}