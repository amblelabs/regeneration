package dev.amble.ars.core;

import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import dev.amble.lib.skin.SkinData;
import dev.amble.lib.skin.SkinTracker;
import dev.amble.ars.RegenerationMod;
import dev.amble.ars.core.animation.AnimationSet;
import dev.amble.ars.core.animation.AnimationTemplate;
import dev.amble.ars.core.animation.RegenAnimRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.lib.animation.AnimatedEntity;
import dev.amble.lib.animation.AnimationTracker;
import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.api.RegenerationEvents;
import dev.amble.ars.data.Attachments;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RegenerationCore {
    public static final Identifier SYNC_PACKET = RegenerationMod.id("sync_info");
    public static final Identifier UPDATE_SKIN_PACKET = RegenerationMod.id("update_skin_setting");
    public static final Identifier RESET_SKIN_PACKET = RegenerationMod.id("reset_skin");
    public static final Identifier UPDATE_TARDIS_MODE_PACKET = RegenerationMod.id("update_tardis_mode");

    public static final int TARDIS_MODE_ENABLED = 0;//随机
    public static final int TARDIS_MODE_DISABLED = 1;//关闭
    public static final int TARDIS_MODE_REFURBISH = 2;//只重构

    private static final String[] REGENERATION_SKINS = new String[] {
            "duzo", "loqor", "drtheo_","jin_mary",
            "classic_account", "portal3i", "winndi",
            "thatrhynoguy", "djaftonrr21", "queknees2",
            "auroranyxs", "grimlyy_", "itzchipdip", "addie_astarr"
    };

    /**
     * 强制所有客户端重新解析该玩家的 GameProfile（从而重新加载皮肤）。
     * 1.20.1 使用 PlayerRemoveS2CPacket 移除，再用 PlayerListS2CPacket 重新添加。
     */
    private static void forceSkinRefresh(ServerPlayerEntity player) {
        var playerManager = player.getServer().getPlayerManager();

        playerManager.sendToAll(new PlayerRemoveS2CPacket(List.of(player.getUuid())));

        playerManager.sendToAll(new PlayerListS2CPacket(
                PlayerListS2CPacket.Action.ADD_PLAYER,
                player
        ));
    }

    public static void init() {
        //记录玩家皮肤/时间领主状态
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            if (player instanceof RegenerationCapable regen) {
                RegenerationCore info = regen.getRegenerationInfo();
                if (info != null) {
                    info.captureBaseSkin(player);
                    info.applySkin(player);
                    info.sync(player, player.getUuid());

                    if (info.isRegenQueued()) {
                        if (!info.start(player)) {
                            info.setRegenQueued(false);
                            info.markDirty();
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            List<Entity> entities = new ArrayList<>(server.getPlayerManager().getPlayerList());
            server.getWorlds().forEach(world -> world.iterateEntities().forEach(entities::add));
            entities.forEach(entity -> {
                if (!(entity instanceof RegenerationCapable regen) || !(entity instanceof LivingEntity living)) return;
                RegenerationCore info = regen.getRegenerationInfo();
                if (info != null && info.isRegenerating()) {
                    info.finish(living);
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity entity = handler.getPlayer();
            if (!(entity instanceof RegenerationCapable regen)) return;
            RegenerationCore info = regen.getRegenerationInfo();
            if (info == null) return;

            if (info.isRegenerating()) {
                // 动画期退出
                info.forceFinish(entity);
            } else if (info.getDelay().isRunning()) {
                // 延缓期退出
                info.stopRegeneration(entity);
                info.setRegenQueued(true);
                info.markDirty();
            }
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            RegenerationCore info = RegenerationCore.get(entity);
            if (info == null) return true;

            if (damageSource.isOf(DamageTypes.GENERIC_KILL)) return true;

            if (info.isActive()) return false;

            if (entity.isRemoved()) return true;

            if (info.getUsesLeft() > 0) {
                return !info.tryStart(entity);
            }
            return true;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            RegenerationCore info = RegenerationCore.get(player);
            if (info != null && info.getDelay().hasEvent()) {
                if (!world.getBlockState(pos).isIn(BlockTags.SNOW)) return ActionResult.PASS;
                info.tryStopDelayEvent(player);
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, player.getSoundCategory(), 0.25F, 1.0F);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            RegenerationCore info = RegenerationCore.get(player);
            if (info != null) {
                info.applySkin(player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_SKIN_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean changeSkin = buf.readBoolean();
            server.execute(() -> {
                RegenerationCore info = RegenerationCore.get(player);
                if (info != null) {
                    info.setChangeSkinOnRegen(changeSkin);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RESET_SKIN_PACKET, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                RegenerationCore info = RegenerationCore.get(player);
                if (info != null) {
                    info.resetSkinToBase(player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_TARDIS_MODE_PACKET, (server, player, handler, buf, responseSender) -> {
            int mode = buf.readInt();
            server.execute(() -> {
                RegenerationCore info = RegenerationCore.get(player);
                if (info != null) {
                    info.setTardisInteriorMode(mode);
                }
            });
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!(newPlayer instanceof RegenerationCapable regen)) return;
            RegenerationCore info = RegenerationCore.get(newPlayer);
            if (info != null) {
                info.stopRegeneration(newPlayer);
                info.sync(newPlayer, newPlayer.getUuid());
            }
        });
    }

    public static String getRandomRegenerationSkin() {
        return REGENERATION_SKINS[(int) (Math.random() * REGENERATION_SKINS.length)];
    }

    public static final Codec<RegenerationCore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("usesLeft").forGetter(RegenerationCore::getUsesLeft),
            Codec.BOOL.fieldOf("isRegenerating").forGetter(RegenerationCore::isRegenerating),
            Codec.BOOL.fieldOf("regenQueued").forGetter(RegenerationCore::isRegenQueued),
            Identifier.CODEC.fieldOf("animation").forGetter(RegenerationCore::getAnimationId),
            Delay.CODEC.fieldOf("delay").forGetter(info -> info.delay),
            Codec.FLOAT.fieldOf("colorR").forGetter(info -> info.particleColor.x()),
            Codec.FLOAT.fieldOf("colorG").forGetter(info -> info.particleColor.y()),
            Codec.FLOAT.fieldOf("colorB").forGetter(info -> info.particleColor.z()),
            Codec.LONG.optionalFieldOf("invulnerableUntil", -1L).forGetter(RegenerationCore::getInvulnerableUntil),
            Codec.LONG.optionalFieldOf("confusedUntil", -1L).forGetter(RegenerationCore::getConfusedUntil),
            Codec.BOOL.optionalFieldOf("changeSkinOnRegen", true).forGetter(RegenerationCore::isChangeSkinOnRegen),
            Codec.BOOL.optionalFieldOf("skinReset", false).forGetter(RegenerationCore::isSkinReset),
            Codec.STRING.optionalFieldOf("overlaySkinId").forGetter(info -> Optional.ofNullable(info.overlaySkinId)),
            Codec.BOOL.optionalFieldOf("useOverlaySkin", false).forGetter(RegenerationCore::isUsingOverlaySkin),
            Codec.BOOL.optionalFieldOf("baseSkinCaptured", false).forGetter(RegenerationCore::isBaseSkinCaptured),
            Codec.INT.optionalFieldOf("tardisInteriorMode", TARDIS_MODE_ENABLED).forGetter(RegenerationCore::getTardisInteriorMode)
    ).apply(instance, (usesLeft, isRegenerating, regenQueued, animationId, delay, r, g, b, invulnUntil, confUntil, changeSkin, skinReset, overlayOpt, useOverlay, baseCaptured, tardisInteriorMode) -> {
        RegenerationCore info = new RegenerationCore(usesLeft, isRegenerating, regenQueued, animationId, delay);
        info.particleColor.set(r, g, b);
        info.invulnerableUntil = invulnUntil;
        info.confusedUntil = confUntil;
        info.changeSkinOnRegen = changeSkin;
        info.skinReset = skinReset;
        info.overlaySkinId = overlayOpt.orElse(null);
        info.useOverlay = useOverlay;
        info.baseSkinCaptured = baseCaptured;
        info.tardisInteriorMode = tardisInteriorMode;
        return info;
    }));

    public static final int MAX_REGENERATIONS = 12;
    private static final int INVULNERABLE_DURATION = 24000;
    private static final int CONFUSION_MIN_TICKS = 1200;
    private static final int CONFUSION_MAX_EXTRA_TICKS = 3601;
    private static final int CONFUSION_EFFECT_INTERVAL_MIN = 100;
    private static final int CONFUSION_EFFECT_INTERVAL_MAX = 300;
    private static final float REGEN_BOOST_MULTIPLIER = 3.0f;

    private int usesLeft;
    private boolean isRegenerating;
    private boolean regenQueued;
    private AnimationTemplate animation;
    private final Delay delay;
    private boolean dirty;
    private final Vector3f particleColor;
    @Nullable
    private AnimationSet currentAnimationSet;

    private long invulnerableUntil;
    private long confusedUntil;
    private int confusionEffectTimer;
    private int regenBoostTimer;

    private boolean changeSkinOnRegen = true;
    private boolean skinReset = false;

    //A/B 双层皮肤状态
    /** A层：是否已记录玩家原始皮肤（进入世界时的皮肤状态） */
    private boolean baseSkinCaptured = false;
    /** B层：重生覆盖皮肤的用户名，null 表示 B层空闲 */
    @Nullable private String overlaySkinId = null;
    /** 当前是否使用 B层（true=B层, false=A层） */
    private boolean useOverlay = false;

    private int tardisInteriorMode = TARDIS_MODE_ENABLED;

    private RegenerationCore(int usesLeft, boolean isRegenerating, boolean regenQueued, Identifier animation, Delay delay) {
        this.usesLeft = usesLeft;
        this.isRegenerating = isRegenerating;
        this.regenQueued = regenQueued;
        this.animation = RegenAnimRegistry.getInstance().getOrFallback(animation);
        this.delay = delay;
        this.particleColor = new Vector3f(1.0f, 1.0f, 1.0f);
        this.invulnerableUntil = -1;
        this.confusedUntil = -1;
        this.confusionEffectTimer = 0;
        this.regenBoostTimer = 0;
    }

    public RegenerationCore() {
        this(0, false, false, RegenAnimRegistry.getInstance().getRandom().id(), new Delay());
    }

    public int getUsesLeft() { return usesLeft; }
    public void setUsesLeft(int usesLeft) {
        this.usesLeft = MathHelper.clamp(usesLeft, 0, MAX_REGENERATIONS);
        this.markDirty();
    }

    public boolean isRegenerating() { return isRegenerating; }
    public void setRegenerating(boolean regenerating) {
        isRegenerating = regenerating;
        this.markDirty();
    }

    public boolean isRegenQueued() { return regenQueued; }
    public void setRegenQueued(boolean regenQueued) {
        this.regenQueued = regenQueued;
        this.markDirty();
    }

    public AnimationTemplate getAnimation() {
        if (this.animation == null) {
            this.animation = RegenAnimRegistry.getInstance().getRandom();
        }
        return animation;
    }

    public void setAnimation(AnimationTemplate animation) {
        this.animation = animation;
        this.markDirty();
    }

    public Delay getDelay() { return delay; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public long getInvulnerableUntil() { return invulnerableUntil; }
    public long getConfusedUntil() { return confusedUntil; }

    public boolean isInvulnerable() { return this.invulnerableUntil > 0; }
    public boolean isConfused() { return this.confusedUntil > 0; }

    public boolean isChangeSkinOnRegen() { return changeSkinOnRegen; }
    public void setChangeSkinOnRegen(boolean value) {
        this.changeSkinOnRegen = value;
        this.markDirty();
    }

    public boolean isSkinReset() { return skinReset; }

    public boolean isBaseSkinCaptured() { return baseSkinCaptured; }
    public boolean isUsingOverlaySkin() { return useOverlay; }

    public int getTardisInteriorMode() { return tardisInteriorMode; }
    public void setTardisInteriorMode(int mode) {
        this.tardisInteriorMode = MathHelper.clamp(mode, 0, 2);
        this.markDirty();
    }

    //预留给 UI 显示当前皮肤名
    @Nullable public String getOverlaySkinId() { return overlaySkinId; }

    public void decrement() {
        this.setUsesLeft(this.getUsesLeft() - 1);
    }

    /**
     * 标记 A 层已捕获（玩家加入世界时的原生皮肤状态）。
     * 实际不需要保存任何数据，因为 A 层 = SkinTracker 里没有该 UUID 的条目。
     */
    public void captureBaseSkin(ServerPlayerEntity player) {
        if (this.baseSkinCaptured) return;
        this.baseSkinCaptured = true;
        this.useOverlay = false;
        this.markDirty();
        RegenerationMod.LOGGER.debug("Base skin captured for {}", player.getUuid());
    }

    public void setOverlaySkin(@Nullable String username) {
        this.overlaySkinId = username;
        this.markDirty();
    }

    public void activateOverlay() {
        if (this.overlaySkinId == null) return;
        this.useOverlay = true;
        this.skinReset = false;
        this.markDirty();
    }

    public void deactivateOverlay() {
        this.useOverlay = false;
        this.skinReset = true;
        this.markDirty();
    }

    /**
     * 根据当前层状态应用皮肤。
     * B层：向 SkinTracker 写入覆盖皮肤，广播给所有客户端。
     * A层：从 SkinTracker 移除该玩家的覆盖数据，广播清除给所有客户端。
     */
    public void applySkin(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (this.useOverlay && this.overlaySkinId != null) {
            // B层：写入 SkinTracker 并广播 SYNC 包
            SkinData.usernameUpload(this.overlaySkinId, uuid);
            RegenerationMod.LOGGER.info("Applied overlay skin {} for {}", this.overlaySkinId, uuid);
        } else {
            // A层：从 SkinTracker 移除并广播 CLEAR 包
            SkinTracker.getInstance().removeSynced(uuid);
            RegenerationMod.LOGGER.info("Removed overlay skin, restored base skin for {}", uuid);
        }

        // 强制客户端重建 PlayerListEntry，触发皮肤重载
        forceSkinRefresh(player);
    }

    public void onTransitionApplySkin(ServerPlayerEntity player, String username) {
        this.setOverlaySkin(username);
        this.activateOverlay();
        this.applySkin(player);
    }

    /**
     * 重置皮肤入口：切回 A 层 + 立即应用 + 同步状态给所有客户端。
     */
    public void resetSkinToBase(ServerPlayerEntity player) {
        this.deactivateOverlay();
        this.applySkin(player);

        // 广播 SYNC_PACKET，让客户端 UI 状态同步
        for (ServerPlayerEntity target : player.getServer().getPlayerManager().getPlayerList()) {
            this.sync(target, player.getUuid());
        }
    }

    //Tick & Logic

    public void tick(LivingEntity entity) {
        if (entity.getWorld().isClient) return;

        // 后备捕获：如果加入时没捕获到（旧存档），tick 里补一次
        if (!this.baseSkinCaptured && entity instanceof ServerPlayerEntity player) {
            this.captureBaseSkin(player);
        }

        if (this.isDirty()) {
            this.setDirty(false);
            for (ServerPlayerEntity player : entity.getWorld().getServer().getPlayerManager().getPlayerList()) {
                this.sync(player, entity.getUuid());
            }
        }

        long worldTime = entity.getWorld().getTime();

        this.tickInvulnerability(entity, worldTime);
        this.tickConfusion(entity, worldTime);

        if (delay.isRunning()) {
            if (this.getUsesLeft() <= 0) {
                delay.stop();
                this.markDirty();
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
                case NONE -> {}
            }
        }

        if (isRegenQueued()) {
            if (!this.start(entity)) {
                this.setRegenQueued(false);
                this.markDirty();
                RegenerationMod.LOGGER.warn("Regeneration start failed for {}, clearing queued state", entity.getUuid());
            }
        }
    }

    private void tickInvulnerability(LivingEntity entity, long worldTime) {
        if (this.invulnerableUntil <= 0) return;

        if (worldTime >= this.invulnerableUntil) {
            this.invulnerableUntil = -1;
            this.markDirty();
            RegenerationMod.LOGGER.info("Invulnerability ended for {}", entity.getUuid());
            return;
        }

        this.tickRegenBoost(entity);
    }

    private void tickRegenBoost(LivingEntity entity) {
        if (entity.getHealth() >= entity.getMaxHealth()) return;
        if (!(entity instanceof PlayerEntity)) return;

        this.regenBoostTimer++;
        int boostedInterval = (int) (80 / REGEN_BOOST_MULTIPLIER);
        if (this.regenBoostTimer >= boostedInterval) {
            this.regenBoostTimer = 0;
            PlayerEntity player = (PlayerEntity) entity;
            if (player.getHungerManager().getFoodLevel() > 0 || player.getWorld().getGameRules().getBoolean(net.minecraft.world.GameRules.NATURAL_REGENERATION)) {
                entity.heal(1.0f);
                if (player.getHungerManager().getFoodLevel() > 0) {
                    player.getHungerManager().addExhaustion(3.0f);
                }
            }
        }
    }

    public float applyDamageReduction(LivingEntity entity, net.minecraft.entity.damage.DamageSource source, float amount) {
        if (!this.isInvulnerable()) return amount;

        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }

        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        float damageMultiplier = MathHelper.lerp(healthPercent, 0.0001f, 0.2f);

        return amount * damageMultiplier;
    }

    private void tickConfusion(LivingEntity entity, long worldTime) {
        if (this.confusedUntil <= 0) return;

        if (worldTime >= this.confusedUntil) {
            this.confusedUntil = -1;
            this.confusionEffectTimer = 0;
            this.markDirty();
            RegenerationMod.LOGGER.info("Confusion ended for {}", entity.getUuid());
            return;
        }

        if (entity.getWorld().isClient) return;

        this.confusionEffectTimer--;
        if (this.confusionEffectTimer > 0) return;

        this.confusionEffectTimer = CONFUSION_EFFECT_INTERVAL_MIN +
                RegenerationMod.RANDOM.nextInt(CONFUSION_EFFECT_INTERVAL_MAX - CONFUSION_EFFECT_INTERVAL_MIN + 1);

        int effectRoll = RegenerationMod.RANDOM.nextInt(100);
        StatusEffectInstance effect = null;

        if (effectRoll < 30) {
            effect = new StatusEffectInstance(
                    StatusEffects.NAUSEA,
                    100 + RegenerationMod.RANDOM.nextInt(100),
                    0, false, false, true
            );
        } else if (effectRoll < 55) {
            effect = new StatusEffectInstance(
                    StatusEffects.DARKNESS,
                    60 + RegenerationMod.RANDOM.nextInt(80),
                    0, false, false, true
            );
        } else if (effectRoll < 75) {
            effect = new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    60 + RegenerationMod.RANDOM.nextInt(80),
                    RegenerationMod.RANDOM.nextInt(2), false, false, true
            );
        } else if (effectRoll < 90) {
            effect = new StatusEffectInstance(
                    StatusEffects.WEAKNESS,
                    60 + RegenerationMod.RANDOM.nextInt(60),
                    0, false, false, true
            );
        } else {
            effect = new StatusEffectInstance(
                    StatusEffects.HUNGER,
                    20 + RegenerationMod.RANDOM.nextInt(40),
                    2 + RegenerationMod.RANDOM.nextInt(3), false, false, true
            );
        }

        if (effect != null) {
            entity.addStatusEffect(effect);
        }

        if (RegenerationMod.RANDOM.nextFloat() < 0.3f) {
            float randomYaw = (RegenerationMod.RANDOM.nextFloat() - 0.5f) * 90f;
            entity.setYaw(entity.getYaw() + randomYaw);
            if (entity instanceof ServerPlayerEntity player) {
                player.networkHandler.requestTeleport(
                        player.getX(), player.getY(), player.getZ(),
                        player.getYaw(), player.getPitch()
                );
            }
        }
    }

    public boolean tryStart(LivingEntity entity) {
        if (this.isActive() || this.isInvulnerable() || this.usesLeft <= 0) return false;
        if (entity.isRemoved()) return false;
        this.delay.start(entity.age);
        this.markDirty();
        entity.setHealth(entity.getMaxHealth());
        if (entity instanceof AnimatedEntity animated) {
            animated.playAnimation(BedrockAnimationReference.parse(Identifier.of("start", RegenerationMod.RANDOM.nextBoolean() ? "right" : "left")));
        }
        RegenerationMod.LOGGER.info("Delay started for {}, will regenerate after {} ticks", entity.getUuid(), Delay.MAX_DURATION);
        return true;
    }

    private boolean start(LivingEntity entity) {
        if (this.isRegenerating() || this.isInvulnerable() || this.usesLeft <= 0) return false;
        if (!entity.isAlive()) return false;

        this.setRegenQueued(false);
        this.decrement();
        this.setRegenerating(true);
        entity.setHealth(entity.getMaxHealth());

        boolean changeSkin = this.changeSkinOnRegen;
        String targetSkin = null;

        if (entity instanceof ServerPlayerEntity player) {
            if (changeSkin) {
                if (!this.baseSkinCaptured) {
                    this.captureBaseSkin(player);
                }
                targetSkin = getRandomRegenerationSkin();
                this.skinReset = false;
                this.markDirty();
            }
        }

        if (entity instanceof AnimatedEntity animated) {
            AnimationTemplate template = RegenAnimRegistry.getInstance().getRandom();
            AnimationSet set = template.instantiate(changeSkin, targetSkin);
            this.currentAnimationSet = set;

            set.finish(() -> {
                RegenerationMod.LOGGER.info("Animation finish callback for {}", entity.getUuid());
                this.finish(entity);
            });
            set.start(animated);
            for (AnimationTemplate.Stage stage : AnimationTemplate.Stage.values()) {
                set.callback(stage, s -> {
                    RegenerationEvents.CHANGE_STAGE.invoker().onStateChange(entity, this, s);
                });
            }
            RegenerationMod.LOGGER.info("Started regeneration animation for {}", entity.getUuid());
        } else {
            Scheduler.get().runTaskLater(() -> {
                RegenerationMod.LOGGER.info("Non-animated entity regeneration finish for {}", entity.getUuid());
                this.finish(entity);
            }, TaskStage.END_SERVER_TICK, TimeUnit.SECONDS, 5);
        }

        RegenerationEvents.START.invoker().onStart(entity, this);
        this.markDirty();
        return true;
    }

    private void finish(LivingEntity entity) {
        RegenerationMod.LOGGER.info("finish() called for {}", entity.getUuid());

        this.stopRegeneration(entity);

        long worldTime = entity.getWorld().getTime();
        this.invulnerableUntil = worldTime + INVULNERABLE_DURATION;
        int confusionDuration = CONFUSION_MIN_TICKS + RegenerationMod.RANDOM.nextInt(CONFUSION_MAX_EXTRA_TICKS);
        this.confusedUntil = worldTime + confusionDuration;
        this.confusionEffectTimer = 0;
        this.regenBoostTimer = 0;

        RegenerationEvents.FINISH.invoker().onFinish(entity, this);
        this.setAnimation(RegenAnimRegistry.getInstance().getRandom());
        this.markDirty();

        entity.setNoGravity(false);
        entity.setVelocity(entity.getVelocity().multiply(0.5));
        entity.updatePosition(entity.getX(), entity.getY(), entity.getZ());

        RegenerationMod.LOGGER.info(
                "Regeneration finished for {}. Dynamic damage reduction + boosted regen active for {} ticks, confused for {} ticks",
                entity.getUuid(), INVULNERABLE_DURATION, confusionDuration
        );
    }

    /**
     * 强制完成重生流程（用于玩家退出等异常中断场景）。
     * 不扣次数（已在 start() 中扣除），直接应用 finish 的所有效果。
     */
    public void forceFinish(LivingEntity entity) {
        finish(entity);
        RegenerationMod.LOGGER.info("Forced regeneration finish for {} due to disconnect", entity.getUuid());
    }

    private void resetAnimationState(LivingEntity entity) {
        if (!(entity instanceof AnimatedEntity animated)) return;

        try {
            animated.getAnimationState().stop();
            AnimationTracker.getInstance().remove(animated.getUuid());
            RegenerationMod.LOGGER.debug("Animation state reset for {}", entity.getUuid());
        } catch (Exception e) {
            RegenerationMod.LOGGER.error("Failed to reset animation state for {}", entity.getUuid(), e);
        }

        this.currentAnimationSet = null;
    }

    private Identifier getAnimationId() {
        return this.getAnimation().id();
    }

    public void stopRegeneration(@Nullable LivingEntity entity) {
        if (this.currentAnimationSet != null) {
            this.currentAnimationSet.cancel();
            this.currentAnimationSet = null;
        }

        if (entity instanceof AnimatedEntity) {
            this.resetAnimationState(entity);
        }

        this.invulnerableUntil = -1;
        this.confusedUntil = -1;
        this.confusionEffectTimer = 0;
        this.regenBoostTimer = 0;

        this.setRegenerating(false);
        this.delay.stop();
        this.markDirty();
        RegenerationMod.LOGGER.debug("Regeneration stopped for (state reset)");
    }

    @Deprecated
    public void stopRegeneration() {
        this.stopRegeneration(null);
    }

    public boolean tryStopDelayEvent(@Nullable LivingEntity entity) {
        if (!this.delay.hasEvent()) return false;
        this.delay.stopEvent();
        this.markDirty();
        RegenerationEvents.DELAY_FURTHER.invoker().onEvent(entity, this);
        return true;
    }

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

    //客户端回调
    @Environment(EnvType.CLIENT)
    public static void receive(PacketByteBuf buf) {
        UUID playerId = buf.readUuid();
        RegenerationCore newInfo = buf.decodeAsJson(CODEC);
        if (newInfo == null) {
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

        entity.setAttached(Attachments.REGENERATION, newInfo);
        entity.setAttached(Attachments.IS_TIMELORD, true);

        RegenerationMod.LOGGER.debug("RegenerationInfo synced to client for {}", playerId);
    }

    public static RegenerationCore get(LivingEntity entity) {
        if (!(entity instanceof RegenerationCapable capability)) return null;
        return capability.getRegenerationInfo();
    }

    public static class Delay {
        public static final Codec<Delay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("start").forGetter(delay -> delay.start),
                Codec.INT.fieldOf("lastEvent").forGetter(delay -> delay.lastEvent)
        ).apply(instance, Delay::new));

        private static final int MAX_DURATION = 6000;
        private static final int TIME_TO_STOP = 300;
        private static final float EVENT_CHANCE = 0.05f;

        private int start;
        private int lastEvent;

        public Delay(int start, int lastEvent) {
            this.start = start;
            this.lastEvent = lastEvent;
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
            if (this.start < 0) return 0;
            float duration = current - this.start;
            if (duration <= 0) return 0;
            if (duration >= MAX_DURATION) return 1;
            return duration / MAX_DURATION;
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

        public Result tick(int current) {
            if (this.start < 0) return Result.NONE;
            if (current < this.start) {
                this.stop();
                return Result.NONE;
            }
            if (current - this.start >= MAX_DURATION) {
                this.stop();
                return Result.REGENERATE;
            }
            if (this.lastEvent > 0 && current - this.lastEvent >= TIME_TO_STOP) {
                this.stop();
                return Result.REGENERATE;
            }
            if (this.lastEvent < 0) {
                float progress = this.getProgress(current);
                float probability = EVENT_CHANCE * progress;
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
            NONE
        }
    }

    //清除时间领主标签发包
    public static final Identifier CLEAR_TIMELORD_PACKET = RegenerationMod.id("clear_timelord");

    @Environment(EnvType.CLIENT)
    public static void receiveClear(PacketByteBuf buf) {
        UUID playerId = buf.readUuid();
        if (net.minecraft.client.MinecraftClient.getInstance().world == null) return;
        PlayerEntity entity = net.minecraft.client.MinecraftClient.getInstance().world.getPlayerByUuid(playerId);
        if (entity == null) return;

        entity.removeAttached(Attachments.REGENERATION);
        entity.setAttached(Attachments.IS_TIMELORD, false);
    }
}