package dev.amble.timelordregen.api;

import dev.amble.timelordregen.animation.AnimationTemplate;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class RegenerationEvents {
	/**
	 * Called when a player starts to regenerate
	 */
	public static final Event<Start> START = EventFactory.createArrayBacked(Start.class, callbacks -> (player, data) -> {
		for (Start callback : callbacks) {
			callback.onStart(player, data);
		}
	});

	/**
	 * Called when a player finishes regeneration
	 */
	public static final Event<Finish> FINISH = EventFactory.createArrayBacked(Finish.class, callbacks -> (player, data) -> {
		for (Finish callback : callbacks) {
			callback.onFinish(player, data);
		}
	});

	/**
	 * Called when a player's regeneration stage changes
	 */
	public static final Event<ChangeStage> CHANGE_STAGE = EventFactory.createArrayBacked(ChangeStage.class, callbacks -> (entity, data, stage) -> {
		for (ChangeStage callback : callbacks) {
			callback.onStateChange(entity, data, stage);
		}
	});

	/**
	 * Called when a player transitions (eg changes skin)
	 * @see AnimationTemplate.TransitionPoint
	 */
	public static final Event<Transition> TRANSITION = EventFactory.createArrayBacked(Transition.class, callbacks -> (entity, data, stage) -> {
		for (Transition callback : callbacks) {
			callback.onTransition(entity, data, stage);
		}
	});

	/**
	 * Called when a regeneration delay event triggers
	 * @see RegenerationInfo.Delay.Result
	 */
	public static final Event<DelayFurther> DELAY_EVENT = EventFactory.createArrayBacked(DelayFurther.class, callbacks -> (entity, data) -> {
		for (DelayFurther callback : callbacks) {
			callback.onEvent(entity, data);
		}
	});

	/**
	 * Called when a regeneration is delayed
	 */
	public static final Event<DelayFurther> DELAY_FURTHER = EventFactory.createArrayBacked(DelayFurther.class, callbacks -> (entity, data) -> {
		for (DelayFurther callback : callbacks) {
			callback.onEvent(entity, data);
		}
	});


	@FunctionalInterface
	public interface Start {
		void onStart(Entity player, RegenerationInfo data);
	}

	@FunctionalInterface
	public interface Finish { // ( Loqor couldnt. )
		void onFinish(Entity player, RegenerationInfo data);
	}

	@FunctionalInterface
	public interface ChangeStage {
		void onStateChange(LivingEntity entity, RegenerationInfo data, AnimationTemplate.Stage stage);
	}

	@FunctionalInterface
	public interface Transition {
		void onTransition(LivingEntity entity, RegenerationInfo data, AnimationTemplate.Stage stage);
	}

	@FunctionalInterface
	public interface DelayFurther {
		void onEvent(@Nullable LivingEntity entity, RegenerationInfo data);
	}
}
