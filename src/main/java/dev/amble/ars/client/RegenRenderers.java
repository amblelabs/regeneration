package dev.amble.ars.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.amble.ars.RegenerationMod;
import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.core.animation.AnimationTemplate;
import dev.amble.lib.animation.AnimatedInstance;
import dev.amble.lib.client.bedrock.BedrockAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public enum RegenRenderers implements RegenRendering {
    PARTICLE {
        @Override
        public void renderArm(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation,
                              RegenerationCore info, Model model, MatrixStack matrices,
                              VertexConsumerProvider provider, float light, Arm arm) {
            // 粒子已移至 ClientRegenParticleManager 统一处理
        }

        @Override
        public void renderAtHead(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation,
                                 RegenerationCore info, Model model, MatrixStack matrices,
                                 VertexConsumerProvider provider, float light, ModelPart headPart) {
            // 粒子已移至 ClientRegenParticleManager 统一处理
        }
    };

    public static final String KEY = "regen_effect";

    public static final Codec<RegenRenderers> CODEC = Codecs.NON_EMPTY_STRING.flatXmap(s -> {
        try {
            return DataResult.success(RegenRenderers.valueOf(s.toUpperCase()));
        } catch (Exception e) {
            return DataResult.error(() -> "Invalid regeneration render type: " + s + "! | " + e.getMessage());
        }
    }, var -> DataResult.success(var.toString()));

    public static void tryRender(AnimatedInstance entity, float progress, Model model, MatrixStack matrices, VertexConsumerProvider provider, float light, @Nullable Arm firstPersonArm) {
        if (!(entity instanceof RegenerationCapable capable)) return;

        capable.withInfo().ifPresent(info -> {
            if (!info.isActive()) return;

            RegenRendering type = RegenRenderers.PARTICLE;

            BedrockAnimation animation;
            try {
                animation = info.getAnimation().get(AnimationTemplate.Stage.START).reference().get().orElseThrow();

                if (animation.metadata.excess().has(KEY)) {
                    String key = animation.metadata.excess().get(KEY).getAsString();
                    type = RegenRenderers.valueOf(key.toUpperCase());
                }
            } catch (Exception e) {
                String validOptions = java.util.Arrays.toString(RegenRenderers.values());
                String errorMsg = "Failed to get regeneration effect type from animation metadata, valid options are: " + validOptions + ". Error: " + e.getMessage();
                RegenerationMod.LOGGER.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }

            matrices.push();
            type.render(entity, progress, animation, info, model, matrices, provider, light, firstPersonArm);
            matrices.pop();
        });
    }
}