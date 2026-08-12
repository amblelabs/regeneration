package ars.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.lib.api.Identifiable;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import ars.RegenerationMod;
import ars.api.RegenerationEvents;
import ars.core.RegenerationCore;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class AnimationTemplate extends EnumMap<AnimationTemplate.Stage, AnimationTemplate.ReferenceWrapper> implements Identifiable {
    public static final Codec<TimeUnit> TIME_UNIT_CODEC = Codecs.NON_EMPTY_STRING.flatXmap(s -> {
        try {
            return DataResult.success(TimeUnit.valueOf(s.toUpperCase()));
        } catch (Exception e) {
            return DataResult.error(() -> "Invalid time unit: " + s + "! | " + e.getMessage());
        }
    }, var -> DataResult.success(var.toString()));

    public static final Codec<AnimationTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Stage.CODEC, ReferenceWrapper.CODEC).fieldOf("stages").forGetter(template -> template),
            TransitionPoint.CODEC.optionalFieldOf("transition").forGetter(AnimationTemplate::getTransitionPoint)
    ).apply(instance, (map, skinChangeOpt) -> {
        AnimationTemplate template = new AnimationTemplate(map);
        skinChangeOpt.ifPresent(skinChange -> template.transition = skinChange);
        return template;
    }));

    private Identifier id;
    @Nullable private AnimationTemplate.TransitionPoint transition;

    public AnimationTemplate() {
        super(Stage.class);
    }

    public AnimationTemplate(ReferenceWrapper start, ReferenceWrapper loop, ReferenceWrapper end) {
        this();
        this.put(Stage.START, start);
        this.put(Stage.LOOP, loop);
        this.put(Stage.END, end);
    }

    public AnimationTemplate(Map<Stage, ReferenceWrapper> map) {
        this();
        this.putAll(map);
    }

    /**
     * 重构：皮肤逻辑移出 AnimationTemplate，由 RegenerationInfo 统一管理。
     * @param skinChange 是否换皮
     * @param overlaySkinName B层皮肤用户名，null 表示不换皮
     */
    public AnimationSet instantiate(boolean skinChange, @Nullable String overlaySkinName) {
        AnimationSet set = new AnimationSet(this);

        if (skinChange && this.transition != null && overlaySkinName != null) {
            set.callback(this.transition.stage(), stage -> {
                Scheduler.get().runTaskLater(() -> {
                    // 皮肤应用逻辑：交给 RegenerationInfo，不再直接操作 SkinData
                    if (set.getTarget() instanceof ServerPlayerEntity player) {
                        RegenerationCore info = RegenerationCore.get(player);
                        if (info != null) {
                            info.onTransitionApplySkin(player, overlaySkinName);
                        }
                    }

                    if (set.getTarget() instanceof LivingEntity living) {
                        RegenerationEvents.TRANSITION.invoker().onTransition(living, RegenerationCore.get(living), this.transition.stage());
                    }
                }, TaskStage.END_SERVER_TICK, this.transition.unit(), this.transition.duration());
            });
        }

        return set;
    }

    @Override
    public Identifier id() {
        if (this.id == null) {
            for (ReferenceWrapper wrapper : this.values()) {
                if (wrapper != null && wrapper.reference() != null) {
                    Identifier found = wrapper.reference().id();
                    this.id = Identifier.of(found.getNamespace(), "regen_template");
                    break;
                }
            }

            int count = 2;
            while (RegenAnimRegistry.getInstance().getOptional(this.id).isPresent()) {
                this.id = Identifier.of(this.id.getNamespace(), this.id.getPath() + "_" + count);
                count++;
            }

            if (this.id == null) {
                throw new IllegalStateException("No identifier for " + this + " - likely no animations set");
            }
        }

        return id;
    }

    public Optional<TransitionPoint> getTransitionPoint() {
        return Optional.ofNullable(this.transition);
    }

    public static AnimationTemplate fromInputStream(InputStream stream) {
        return fromJson(JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject());
    }

    public static AnimationTemplate fromJson(JsonObject json) {
        AtomicReference<AnimationTemplate> created = new AtomicReference<>();

        CODEC.decode(JsonOps.INSTANCE, json).get().ifLeft(var -> created.set(var.getFirst())).ifRight(err -> {
            created.set(null);
            RegenerationMod.LOGGER.error("Error decoding datapack animation template: {}", err);
        });

        return created.get();
    }

    public enum Stage {
        START,
        LOOP,
        END;

        public static final Codec<Stage> CODEC = Codecs.NON_EMPTY_STRING.flatXmap(s -> {
            try {
                return DataResult.success(Stage.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                return DataResult.error(() -> "Invalid state: " + s + "! | " + e.getMessage());
            }
        }, var -> DataResult.success(var.toString()));

        public Stage next() {
            return switch (this) {
                case START -> LOOP;
                case LOOP -> END;
                case END -> null;
            };
        }
    }

    public record ReferenceWrapper(BedrockAnimationReference reference, long duration, TimeUnit unit) {
        public static final Codec<ReferenceWrapper> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                        BedrockAnimationReference.CODEC.fieldOf("reference").forGetter(ReferenceWrapper::reference),
                        Codec.LONG.fieldOf("duration").forGetter(ReferenceWrapper::duration),
                        TIME_UNIT_CODEC.fieldOf("unit").forGetter(ReferenceWrapper::unit))
                .apply(instance, ReferenceWrapper::new));
    }

    public record TransitionPoint(Stage stage, long duration, TimeUnit unit) {
        public static final Codec<TransitionPoint> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                        Stage.CODEC.fieldOf("stage").forGetter(TransitionPoint::stage),
                        Codec.LONG.fieldOf("duration").forGetter(TransitionPoint::duration),
                        TIME_UNIT_CODEC.fieldOf("unit").forGetter(TransitionPoint::unit))
                .apply(instance, TransitionPoint::new));
    }
}