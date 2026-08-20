package dev.amble.timelordregen.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.amble.lib.animation.AnimatedInstance;
import dev.amble.lib.client.bedrock.BedrockAnimation;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.animation.AnimationTemplate;
import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.api.RegenerationInfo;
import dev.amble.timelordregen.client.util.BoneParticleSpawner;
import dev.amble.timelordregen.client.util.BoneSpawnPoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public enum RegenRenderers implements RegenRendering {
	PARTICLE {
		@Override
		public void renderArm(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation, RegenerationInfo info, Model model, MatrixStack matrices, VertexConsumerProvider provider, float light, Arm arm) {
			if (!(entity instanceof LivingEntity livingEntity)) return;
			MinecraftClient client = MinecraftClient.getInstance();
			ClientWorld world = client.world;
			if (world == null) return;

			// Get the arm ModelPart from the model
			ModelPart armPart = getArmPart(model, arm);
			if (armPart == null) return;

			// Use the appropriate spawn point based on which arm
			BoneSpawnPoint spawnPoint = (arm == Arm.RIGHT) ? BoneSpawnPoint.RIGHT_ARM : BoneSpawnPoint.LEFT_ARM;

			float speed = BoneParticleSpawner.getSpeedForAnimation(animation);
			BoneParticleSpawner.spawnAtBone(world, livingEntity, armPart, spawnPoint, speed);
		}

		@Override
		public void renderAtHead(AnimatedInstance entity, float progress, @Nullable BedrockAnimation animation, RegenerationInfo info, Model model, MatrixStack matrices, VertexConsumerProvider provider, float light, ModelPart headPart) {
			if (!(entity instanceof LivingEntity livingEntity)) return;
			MinecraftClient client = MinecraftClient.getInstance();
			ClientWorld world = client.world;
			if (world == null) return;

			float speed = BoneParticleSpawner.getSpeedForAnimation(animation);
			BoneParticleSpawner.spawnAtBone(world, livingEntity, headPart, BoneSpawnPoint.HEAD, speed);
		}

		/**
		 * Gets the arm ModelPart from the model.
		 */
		@Nullable
		private ModelPart getArmPart(Model model, Arm arm) {
			if (model instanceof BipedEntityModel<?> biped) {
				return arm == Arm.RIGHT ? biped.rightArm : biped.leftArm;
			}
			return null;
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
			// Only render particles during actual regeneration, not during delay
			if (!info.isRegenerating()) return;

			RegenRendering type = RegenRenderers.PARTICLE;

			// Get the currently playing animation from the entity, not from the template
			BedrockAnimation animation = null;
			try {
				// First try to get the current animation from the animated entity
				var currentRef = entity.getCurrentAnimation();
				if (currentRef != null) {
					animation = currentRef.get().orElse(null);
				}

				// If no current animation, fall back to START from template
				if (animation == null) {
					var wrapper = info.getAnimation().get(AnimationTemplate.Stage.START);
					if (wrapper != null && wrapper.reference() != null) {
						animation = wrapper.reference().get().orElse(null);
					}
				}

				// Check for custom render type in animation metadata
				if (animation != null && animation.metadata != null && animation.metadata.excess() != null) {
					if (animation.metadata.excess().has(KEY)) {
						String key = animation.metadata.excess().get(KEY).getAsString();
						type = RegenRenderers.valueOf(key.toUpperCase());
					}
				}
			} catch (Exception e) {
				// Log but don't throw - gracefully degrade to default particle rendering
				RegenerationMod.LOGGER.debug("Could not get animation for regen effect: {}", e.getMessage());
			}

			matrices.push();
			type.render(entity, progress, animation, info, model, matrices, provider, light, firstPersonArm);
			matrices.pop();
		});
	}
}
