package dev.amble.ars.client;

import dev.amble.lib.animation.AnimatedInstance;
import dev.amble.lib.client.bedrock.BedrockAnimation;
import dev.amble.ars.core.RegenerationCore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface RegenRendering {
	void renderArm(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation, RegenerationCore info, Model model, MatrixStack matrices, VertexConsumerProvider provider, float light, Arm arm);
	void renderAtHead(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation, RegenerationCore info, Model model, MatrixStack matrices, VertexConsumerProvider provider, float light, ModelPart headPart);

	default void render(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation, RegenerationCore info, Model model, MatrixStack matrices, VertexConsumerProvider provider, float light, @Nullable Arm firstPersonArm) {
		if (model instanceof ModelWithArms armed) {
			for (Arm arm : Arm.values()) {
				if (firstPersonArm != null && arm != firstPersonArm) continue;

				matrices.push();
				armed.setArmAngle(arm, matrices);
				renderArm(entity, progress, animation, info, model, matrices, provider, light, arm);
				matrices.pop();
			}
		}

		if (model instanceof ModelWithHead head && info.isRegenerating() && firstPersonArm == null) {
			matrices.push();
			ModelPart modelPart = head.getHead();
			modelPart.rotate(matrices);
			HeadFeatureRenderer.translate(matrices, false);
			renderAtHead(entity, progress, animation, info, model, matrices, provider, light, modelPart);
			matrices.pop();
		}
	}
}
