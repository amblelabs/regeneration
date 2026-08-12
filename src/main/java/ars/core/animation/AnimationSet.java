package ars.core.animation;

import dev.amble.lib.animation.AnimatedEntity;
import dev.amble.lib.animation.AnimationTracker;
import ars.RegenerationMod;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

public class AnimationSet {
    private final AnimationTemplate template;
    private final EnumMap<AnimationTemplate.Stage, List<Consumer<AnimationTemplate.Stage>>> callbacks;
    private final List<Runnable> finishCallbacks;
    private AnimationTemplate.Stage current;
    private boolean cancelled = false;
    private boolean finished = false;
    @Getter
    @Nullable private AnimatedEntity target;

    public AnimationSet(AnimationTemplate template) {
        this.template = template;
        this.callbacks = new EnumMap<>(AnimationTemplate.Stage.class);
        this.finishCallbacks = new ArrayList<>();
        this.current = AnimationTemplate.Stage.START;
    }

    private AnimationTemplate.ReferenceWrapper get(AnimationTemplate.Stage stage) {
        return this.template.get(stage);
    }

    public void callback(AnimationTemplate.Stage stage, Consumer<AnimationTemplate.Stage> callback) {
        this.callbacks.computeIfAbsent(stage, s -> new ArrayList<>()).add(callback);
    }

    public void finish(Runnable callback) {
        this.finishCallbacks.add(callback);
    }

    private void runCallbacks(AnimationTemplate.Stage stage) {
        List<Consumer<AnimationTemplate.Stage>> cbs = this.callbacks.get(stage);
        if (cbs != null) {
            for (Consumer<AnimationTemplate.Stage> cb : cbs) {
                try {
                    cb.accept(stage);
                } catch (Exception e) {
                    RegenerationMod.LOGGER.warn("Error in AnimationSet callback for stage {}: {}", stage, e.getMessage());
                }
            }
        }
    }

    private void setStage(AnimationTemplate.Stage stage) {
        this.current = stage;
        this.runCallbacks(stage);
    }

    @Nullable
    private AnimationTemplate.Stage nextStage(AnimationTemplate.Stage current) {
        return current.next();
    }

    public void start(AnimatedEntity target) {
        this.target = target;
        this.cancelled = false;
        this.finished = false;
        this.setStage(AnimationTemplate.Stage.START);
        this.runStage(this.current);
    }

    private void runStage(AnimationTemplate.Stage stage) {
        if (this.cancelled || this.target == null) {
            this.finishAnimation();
            return;
        }

        if (stage == null) {
            this.finishAnimation();
            return;
        }

        AnimationTemplate.ReferenceWrapper wrapper = this.get(stage);
        if (wrapper == null) {
            RegenerationMod.LOGGER.warn("No animation reference for stage {}, skipping", stage);
            AnimationTemplate.Stage next = this.nextStage(stage);
            if (next != null) {
                this.setStage(next);
                this.runStage(next);
            } else {
                this.finishAnimation();
            }
            return;
        }

        long duration = wrapper.duration();
        if (duration <= 0) {
            RegenerationMod.LOGGER.warn("Invalid duration {} for stage {}, skipping", duration, stage);
            AnimationTemplate.Stage next = this.nextStage(stage);
            if (next != null) {
                this.setStage(next);
                this.runStage(next);
            } else {
                this.finishAnimation();
            }
            return;
        }

        this.target.playAnimation(wrapper.reference());

        Scheduler.get().runTaskLater(() -> {
            if (this.cancelled || this.target == null) {
                this.finishAnimation();
                return;
            }
            AnimationTemplate.Stage next = this.nextStage(stage);
            if (next != null) {
                this.setStage(next);
                this.runStage(next);
            } else {
                this.finishAnimation();
            }
        }, TaskStage.END_SERVER_TICK, wrapper.unit(), duration);
    }

    /**
     * 执行所有 finish 回调并清理状态
     * FIX: 确保动画状态被正确停止，防止实体卡在动画中
     */
    private void finishAnimation() {
        if (this.finished) return;
        this.finished = true;
        this.cancelled = true;

        // FIX: 停止目标实体的动画状态，防止残留
        if (this.target != null) {
            try {
                this.target.getAnimationState().stop();
                AnimationTracker.getInstance().remove(this.target.getUuid());
                RegenerationMod.LOGGER.debug("Animation state stopped for {}", this.target.getUuid());
            } catch (Exception e) {
                RegenerationMod.LOGGER.error("Failed to stop animation state in finishAnimation", e);
            }
        }

        RegenerationMod.LOGGER.info("AnimationSet finishing with {} callbacks", finishCallbacks.size());
        for (Runnable cb : new ArrayList<>(this.finishCallbacks)) {
            try {
                cb.run();
            } catch (Exception e) {
                RegenerationMod.LOGGER.error("Error in AnimationSet finish callback", e);
            }
        }
        this.finishCallbacks.clear();
        this.callbacks.clear();
        this.target = null;
    }

    /**
     * 取消动画（也会触发 finish）
     * FIX: 确保取消时也会清理动画状态
     */
    public void cancel() {
        if (this.finished) return;
        RegenerationMod.LOGGER.debug("AnimationSet cancelled");
        this.finishAnimation();
    }
}