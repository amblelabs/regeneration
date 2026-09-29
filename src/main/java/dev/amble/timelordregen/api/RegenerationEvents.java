package dev.amble.timelordregen.api;

import dev.amble.timelordregen.block.RegenerationModBlocks;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.core.animation.AnimationTemplate;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
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
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            ItemStack stack = player.getStackInHand(hand);
            Item item = stack.getItem();

            // 处理斧头剥皮
            if (item instanceof AxeItem) {
                Block newBlock = null;

                if (block == RegenerationModBlocks.CADON_LOG) {
                    newBlock = RegenerationModBlocks.STRIPPED_CADON_LOG;
                } else if (block == RegenerationModBlocks.CADON_WOOD) {
                    newBlock = RegenerationModBlocks.STRIPPED_CADON_WOOD;
                }

                if (newBlock != null) {
                    if (!world.isClient) {
                        BlockState newState = newBlock.getDefaultState();
                        if (state.contains(Properties.AXIS)) {
                            newState = newState.with(Properties.AXIS, state.get(Properties.AXIS));
                        }
                        world.setBlockState(pos, newState);
                        world.playSound(null, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                    }
                    return ActionResult.SUCCESS;
                }
            }

            // 处理锄头锄地
            if (item instanceof HoeItem) {
                if (block == RegenerationModBlocks.GALLIFREY_GRASS_BLOCK) {
                    if (!world.isClient) {
                        // 替换为原版耕地（或自定义耕地）
                        world.setBlockState(pos, Blocks.FARMLAND.getDefaultState(), 3);
                        world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                    }
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });
    }}
