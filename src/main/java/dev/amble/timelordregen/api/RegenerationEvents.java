package dev.amble.timelordregen.api;

import dev.amble.timelordregen.block.RegenerationModBlocks;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.core.animation.AnimationTemplate;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
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
	 * @see RegenerationCore.Delay.Result
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
		void onStart(Entity player, RegenerationCore data);
	}

	@FunctionalInterface
	public interface Finish { // ( Loqor couldnt. )
		void onFinish(Entity player, RegenerationCore data);
	}

	@FunctionalInterface
	public interface ChangeStage {
		void onStateChange(LivingEntity entity, RegenerationCore data, AnimationTemplate.Stage stage);
	}

	@FunctionalInterface
	public interface Transition {
		void onTransition(LivingEntity entity, RegenerationCore data, AnimationTemplate.Stage stage);
	}

	@FunctionalInterface
	public interface DelayFurther {
		void onEvent(@Nullable LivingEntity entity, RegenerationCore data);
	}
    public static void registerListeners() {
        // 剥皮功能：用斧头右键点击原木/木头
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            PlayerEntity entity = player;
            ItemStack stack = entity.getStackInHand(hand);

            // 检查是否手持斧头
            if (!(stack.getItem() instanceof AxeItem)) {
                return ActionResult.PASS;
            }

            Block newBlock = null;

            // 判断当前方块，映射到去皮版本
            if (block == RegenerationModBlocks.CADON_LOG) {
                newBlock = RegenerationModBlocks.STRIPPED_CADON_LOG;
            } else if (block == RegenerationModBlocks.CADON_WOOD) {
                newBlock = RegenerationModBlocks.STRIPPED_CADON_WOOD;
            }

            if (newBlock == null) {
                return ActionResult.PASS;
            }

            // 执行替换
            if (!world.isClient) {
                // 保留原方块的 Axis 属性（使用 Properties.AXIS）
                BlockState newState = newBlock.getDefaultState();
                if (state.contains(Properties.AXIS)) {
                    newState = newState.with(Properties.AXIS, state.get(Properties.AXIS));
                }
                world.setBlockState(pos, newState);
                world.playSound(null, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
                // 消耗耐久（服务端）
                stack.damage(1, entity, p -> p.sendToolBreakStatus(hand));
            }

            return ActionResult.SUCCESS;
        });
    }
}
