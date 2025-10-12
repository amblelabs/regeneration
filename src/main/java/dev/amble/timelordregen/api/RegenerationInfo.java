package dev.amble.timelordregen.api;

import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.advancement.RegenerationCriterions;
import dev.amble.timelordregen.animation.AnimationSet;
import dev.amble.timelordregen.animation.AnimationTemplate;
import dev.amble.timelordregen.animation.RegenAnimRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.lib.animation.AnimatedEntity;
import dev.amble.timelordregen.data.Attachments;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegenerationInfo {
	public static final Identifier SYNC_PACKET = RegenerationMod.id("sync_info");

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			List<Entity> entities = new ArrayList<>(server.getPlayerManager().getPlayerList());
			server.getWorlds().forEach(world -> world.iterateEntities().forEach(entities::add));
			entities.forEach(entity -> {
				if (!(entity instanceof RegenerationCapable regen) || !(entity instanceof LivingEntity living)) return;

				RegenerationInfo info = regen.getRegenerationInfo();
				if (info != null && info.isRegenerating()) {
					info.finish(living);
				}
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity entity = handler.getPlayer();
			if (!(entity instanceof RegenerationCapable regen)) return;

			RegenerationInfo info = regen.getRegenerationInfo();
			if (info != null && info.isRegenerating()) {
				info.setRegenQueued(true);
				info.stopRegeneration();
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity entity = handler.getPlayer();
			if (!(entity instanceof RegenerationCapable regen)) return;

			RegenerationInfo info = regen.getRegenerationInfo();
			if (info != null && info.isRegenQueued()) {
				info.tryStart(entity);
			}
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
			RegenerationInfo info = RegenerationInfo.get(entity);

			if (info == null) return true;

			return !info.tryStart(entity) || info.isActive();
		});

		// hitting snow stops event
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			RegenerationInfo info = RegenerationInfo.get(player);

			if (info != null && info.getDelay().hasEvent()) {
				// check if snow
				if (!world.getBlockState(pos).isIn(BlockTags.SNOW)) return ActionResult.PASS;

				info.tryStopDelayEvent(player);
				world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, player.getSoundCategory(), 0.25F, 1.0F);

				return ActionResult.SUCCESS;
			}

			return ActionResult.PASS;
		});
	}

	public static final Codec<RegenerationInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("usesLeft").forGetter(RegenerationInfo::getUsesLeft),
			Codec.BOOL.fieldOf("isRegenerating").forGetter(RegenerationInfo::isRegenerating),
			Codec.BOOL.fieldOf("regenQueued").forGetter(RegenerationInfo::isRegenQueued),
			Identifier.CODEC.fieldOf("animation").forGetter(RegenerationInfo::getAnimationId),
			Delay.CODEC.fieldOf("delay").forGetter(info -> info.delay)
	).apply(instance, RegenerationInfo::new));

	public static final int MAX_REGENERATIONS = 12;

	@Getter
	private int usesLeft;
	@Getter @Setter
	private boolean isRegenerating;
	@Setter
	private AnimationTemplate animation;
	@Getter @Setter
	private boolean regenQueued; // for when a player leaves and rejoins while regenerating
	@Getter
	private final Delay delay;
	@Getter @Setter
	private boolean dirty; // mark dirty to sync to client

	private RegenerationInfo(int usesLeft, boolean isRegenerating, boolean regenQueued, Identifier animation, Delay delay) {
		this.usesLeft = usesLeft;
		this.isRegenerating = isRegenerating;
		this.regenQueued = regenQueued;
		this.animation = RegenAnimRegistry.getInstance().getOrFallback(animation);
		this.delay = delay;
	}

	/**
	 * Default constructor for creating a new RegenerationInfo
	 */
	public RegenerationInfo() {
		this(0, false, false, RegenAnimRegistry.getInstance().getRandom().id(), new Delay());
	}

	public void setUsesLeft(int usesLeft) {
		this.usesLeft = MathHelper.clamp(usesLeft, 0, MAX_REGENERATIONS);
		this.markDirty();
	}

	public void decrement() {
		this.setUsesLeft(this.getUsesLeft() - 1);
	}

	public void tick(LivingEntity entity) {
		if (entity.getWorld().isClient) return;

		if (this.isDirty()) {
			this.setDirty(false);

			if (entity instanceof ServerPlayerEntity player) {
				this.sync(player, entity.getUuid());
			} else {
				// sync to all players tracking this entity
				entity.getWorld().getPlayers().forEach(player -> {
					if (player.squaredDistanceTo(entity) < 64 * 64) {
						this.sync((ServerPlayerEntity) player, entity.getUuid());
					}
				});
			}
		}

		if (delay.isRunning()) {
			if (this.getUsesLeft() <= 0) {
				delay.stop();
				return;
			}

			Delay.Result result = delay.tick(entity.age);

			switch (result) {
				case REGENERATE -> {
					this.setRegenQueued(true);
					delay.stop();
					this.markDirty();
				}
				case EVENT -> {
					RegenerationEvents.DELAY_EVENT.invoker().onEvent(entity, this);

					this.markDirty();
				}
				case NONE -> {
				}
			}
		}

		if (isRegenQueued()) {
			start(entity);
		}
	}

	public boolean tryStart(LivingEntity entity) {
		if (this.isActive() || this.usesLeft <= 0) return false;

		this.delay.start(entity.age);
		this.markDirty();

		entity.setHealth(entity.getMaxHealth());

		if (entity instanceof AnimatedEntity animated) {
			animated.playAnimation(BedrockAnimationReference.parse(Identifier.of("start", RegenerationMod.RANDOM.nextBoolean() ? "right" : "left")));
		}

		return true;
	}

	private boolean start(LivingEntity entity) {
		if (this.isRegenerating() || this.usesLeft <= 0) return false;

		boolean moving = entity.getX() != entity.prevX || entity.getY() != entity.prevY || entity.getZ() != entity.prevZ || !entity.isOnGround();
		if (moving) return false;

		this.setRegenQueued(false);
		this.decrement();
		this.setRegenerating(true);

		entity.setHealth(entity.getMaxHealth());

		if (entity instanceof AnimatedEntity animated) {
			AnimationSet set = this.getAnimation().instantiate(true); // todo config option for skin change
			set.finish(() -> this.finish(entity));
			set.start(animated);

			for (AnimationTemplate.Stage stage : AnimationTemplate.Stage.values()) {
				set.callback(stage, s -> {
					RegenerationEvents.CHANGE_STAGE.invoker().onStateChange(entity, this, s);
				});
			}
		} else {
			Scheduler.get().runTaskLater(() -> this.finish(entity), TaskStage.END_SERVER_TICK, TimeUnit.SECONDS, 5);
		}

		RegenerationEvents.START.invoker().onStart(entity, this);
		this.markDirty();

		return true;
	}

	private void finish(LivingEntity entity) {
		this.stopRegeneration();
		RegenerationEvents.FINISH.invoker().onFinish(entity, this);

		this.setAnimation(RegenAnimRegistry.getInstance().getRandom());
		this.markDirty();
	}

	private Identifier getAnimationId() {
		return this.getAnimation().id();
	}

	public AnimationTemplate getAnimation() {
		if (this.animation == null) {
			this.animation = RegenAnimRegistry.getInstance().getRandom();
		}

		return animation;
	}

	public void stopRegeneration() {
		this.setRegenerating(false);
		this.delay.stop();
		this.markDirty();
	}

	public boolean tryStopDelayEvent(@Nullable LivingEntity entity) {
		if (!this.delay.hasEvent()) return false;

		this.delay.stopEvent();
		this.markDirty();

		RegenerationEvents.DELAY_FURTHER.invoker().onEvent(entity, this);

		return true;
	}

	/**
	 * Whether the entity is currently in the process of regenerating
	 * @return true if regenerating or in delay or queued to regen
	 */
	public boolean isActive() {
		return this.isRegenerating() || this.delay.isRunning() || this.isRegenQueued();
	}

	public void markDirty() {
		this.setDirty(true);
	}

	private void sync(ServerPlayerEntity target, UUID sourceId) {
		PacketByteBuf buf = PacketByteBufs.create();

		buf.writeUuid(sourceId);
		buf.encodeAsJson(CODEC, this);

		ServerPlayNetworking.send(target, SYNC_PACKET, buf);
	}

	@Environment(EnvType.CLIENT)
	public static void receive(PacketByteBuf buf) {
		UUID playerId  = buf.readUuid();
		RegenerationInfo info = buf.decodeAsJson(CODEC);

		if (info == null) {
			RegenerationMod.LOGGER.warn("Received null RegenerationInfo from server for player {}", playerId);
			return;
		}
		if (net.minecraft.client.MinecraftClient.getInstance().world == null) {
			RegenerationMod.LOGGER.warn("Received RegenerationInfo from server for player {}, but client world is null", playerId);
			return;
		}

		PlayerEntity entity = net.minecraft.client.MinecraftClient.getInstance().world.getPlayerByUuid(playerId);
		if (entity == null) {
			RegenerationMod.LOGGER.warn("Received RegenerationInfo from server for player {}, but could not find player in client world", playerId);
			return;
		}
		if (!(entity instanceof RegenerationCapable)) {
			RegenerationMod.LOGGER.warn("Received RegenerationInfo from server for player {}, but player is not RegenerationCapable", playerId);
			return;
		}
		entity.setAttached(Attachments.REGENERATION, info);
	}

	public static RegenerationInfo get(LivingEntity entity) {
		if (!(entity instanceof RegenerationCapable capability)) return null;

		return capability.getRegenerationInfo();
	}

	public static class Delay {
		public static final Codec<Delay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("start").forGetter(delay -> delay.start),
				Codec.INT.fieldOf("lastEvent").forGetter(delay -> delay.lastEvent)
		).apply(instance, Delay::new));

		private static final int MAX_DURATION = 6000; // 5 minutes
		private static final int TIME_TO_STOP = 300; // time to stop the event
		private static final float EVENT_CHANCE = 0.05f; // max chance of an event happening

		private int start;
		private int lastEvent;

		public Delay(int start, int lastEvent) {
			this.start = start;
			this.lastEvent = lastEvent;
		}

		public Delay(int start) {
			this(start, -1);
		}

		public Delay() {
			this(-1, -1);
		}

		public boolean isRunning() {
			return this.start >= 0;
		}

		public boolean hasEvent() {
			return this.lastEvent >= 0;
		}

		public float getProgress(float current) {
			// chance increases as it approaches max duration
			if (this.start < 0) return 0;
			float duration = current - this.start;
			if (duration <= 0) return 0;
			if (duration >= MAX_DURATION) return 1;
			return (float) duration / MAX_DURATION;
		}

		public float getEventProgress(float current) {
			if (this.lastEvent < 0) return 0;
			float duration = current - this.lastEvent;
			if (duration <= 0) return 0;
			if (duration >= TIME_TO_STOP) return 1;
			return duration / TIME_TO_STOP;
		}

		public void stopEvent() {
			this.lastEvent = -1;
		}

		public void stop() {
			this.start = -1;
			this.lastEvent = -1;
		}

		public void start(int current) {
			this.start = current;
		}

		/**
		 * @return the result of the tick
		 */
		public Result tick(int current) {
			if (this.start < 0) return Result.NONE;
			if (current < this.start) {
				this.stop();
				return Result.NONE;
			}
			if (current - this.start >= MAX_DURATION) {
				this.start = -1;
				return Result.REGENERATE;
			}
			if (this.lastEvent > 0 && current - this.lastEvent >= TIME_TO_STOP) {
				this.start = -1;
				this.lastEvent = -1;
				return Result.REGENERATE;
			}

			if (this.lastEvent < 0/* && current % 20 == 0*/) { // check for event every second
				float progress = this.getProgress(current);
				float probability = EVENT_CHANCE * progress; // Event chance increases with progress
				if (Math.random() < probability) {
					this.lastEvent = current;
					return Result.EVENT;
				}
			}

			return Result.NONE;
		}

		public enum Result {
			REGENERATE,
			EVENT,
			NONE;
		}
	}
}
