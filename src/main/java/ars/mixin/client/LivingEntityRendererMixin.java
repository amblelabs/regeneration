package ars.mixin.client;

import dev.amble.lib.animation.AnimatedInstance;
import ars.client.ClientRegenParticleManager;
import ars.client.RegenRenderers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    public abstract M getModel();

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void regen$afterRender(T livingEntity, float f, float delta, MatrixStack stack,
                                   VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        // ★ 第三人称粒子：自己构建矩阵，不再依赖 stack
        ClientRegenParticleManager.trySpawnForEntity(livingEntity, delta);

        // 保留旧的 RegenRenderers 调用（非粒子特效）
        if (livingEntity instanceof AnimatedInstance animated) {
            RegenRenderers.tryRender(animated, animated.getAge() + delta, this.getModel(), stack, vertexConsumerProvider, i, null);
        }
    }
}