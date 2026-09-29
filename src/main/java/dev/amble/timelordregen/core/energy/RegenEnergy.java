package dev.amble.timelordregen.core.energy;

import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.compat.ait.TardisCompatBridge;
import dev.amble.timelordregen.config.RegenerationServerConfig;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.core.RegenerationExplosion;
import dev.amble.timelordregen.data.Attachments;
import dev.amble.timelordregen.network.EnergyNetworking;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class RegenEnergy {
    public static final RegistryKey<DamageType> DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, RegenerationMod.id("regeneration_energy"));
    public static final RegistryKey<DamageType> DAMAGE_ARMORED = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, RegenerationMod.id("regeneration_energy_armored"));
    public static final TagKey<Block> IMMUNE = TagKey.of(RegistryKeys.BLOCK, RegenerationMod.id("regen_energy_immune"));
    public static final GameRules.Key<GameRules.BooleanRule> DESTROYS_BLOCKS = GameRuleRegistry.register("regenEnergyDestroysBlocks", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static void init() {
        RegenerationServerConfig.INSTANCE.load();
        EnergyNetworking.registerServer();

        ServerLivingEntityEvents.AFTER_DEATH.register(RegenEnergy::onDeath);
        ServerTickEvents.END_WORLD_TICK.register(RegenEnergy::restore);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            EnergyNetworking.sendRules(p);
            EnergyNetworking.sendSync(p, get(p));
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> EnergyNetworking.sendSync(newPlayer, get(newPlayer)));

        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (!(tracked instanceof ServerPlayerEntity caster) || channel(caster) == null) return;
            Scheduler.get().runTaskLater(() -> {
                EnergyChannel c = channel(caster);
                if (c == null || caster.isRemoved() || viewer.isDisconnected()) return;
                EnergyNetworking.sendChannelTo(viewer, caster, c.ability, c.phase, c.targetId);
            }, TaskStage.END_SERVER_TICK, TimeUnit.TICKS, 1);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            stop(player, false);
            EnergyNetworking.sendSync(player, get(player));
        });
    }

    public static void onDisconnect(ServerPlayerEntity p) {
        stop(p, false);
        EnergyChannel.forget(p);
    }

    public static boolean home(World w) {
        TardisCompatBridge b = RegenerationExplosion.getTardisBridge();
        return b != null && b.isTardis(w);
    }

    public static boolean safe(World w) {
        return RegenerationServerConfig.get().tardisSafe && home(w);
    }

    public static void syncRules(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) EnergyNetworking.sendRules(p);
    }

    private static void restore(ServerWorld w) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        if (!c.tardisRestore || w.getTime() % Math.max(1, c.tardisRestoreTicks) != 0 || !home(w)) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            int e = get(p);
            if (e >= c.energyCap || !(p instanceof RegenerationCapable rc) || !rc.isTimelord()) continue;
            int add = Math.min(Math.max(1, c.tardisRestoreAmount), c.energyCap - e);
            if (RegenerationExplosion.getTardisBridge().drawFuel(w, c.tardisFuelPerEnergy * add)) set(p, e + add);
        }
    }

    public static boolean isEnergy(DamageSource source) {
        return source.isOf(DAMAGE) || source.isOf(DAMAGE_ARMORED);
    }

    public static void hurt(ServerPlayerEntity attacker, Entity target, float amount) {
        target.damage(source(attacker), target instanceof PlayerEntity ? amount * RegenerationServerConfig.get().playerDamage : amount);
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayerEntity p) stop(p, false);
        if (!isEnergy(source) || !(entity.getWorld() instanceof ServerWorld world) || entity instanceof EnderDragonEntity) return;

        EnergyNetworking.sendFx(world, entity.getBoundingBox().getCenter(), EnergyFxType.DISINTEGRATE, entity.getId(), entity.getWidth(), entity.getHeight());
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, entity.getSoundCategory(), 1.0f, 1.6f);
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, entity.getSoundCategory(), 1.0f, 1.6f);

        if (!(entity instanceof PlayerEntity) && RegenerationServerConfig.get().burnLoot) entity.discard();
    }

    public static int get(PlayerEntity p) {
        return p.getAttachedOrElse(Attachments.REGEN_ENERGY, 0);
    }

    public static void set(ServerPlayerEntity p, int v) {
        int e = Math.max(0, v);
        p.setAttached(Attachments.REGEN_ENERGY, e);
        EnergyNetworking.sendSync(p, e);
    }

    public static boolean spend(ServerPlayerEntity p, int n) {
        int e = get(p);
        if (e < n) return false;
        set(p, e - n);
        return true;
    }

    public static boolean crack(ServerPlayerEntity p) {
        RegenerationCore info = RegenerationCore.get(p);
        if (info == null || !p.isAlive() || p.isSpectator()) return false;
        if (info.isActive()) {
            p.sendMessage(Text.translatable("message.timelordregen.energy.busy"), true);
            return false;
        }
        if (info.getUsesLeft() < 1) {
            p.sendMessage(Text.translatable("message.timelordregen.energy.no_regens"), true);
            return false;
        }
        int e = get(p);
        int per = RegenerationServerConfig.get().energyPerRegen;
        if (e + per > RegenerationServerConfig.get().energyCap) {
            p.sendMessage(Text.translatable("message.timelordregen.energy.full"), true);
            return false;
        }

        info.decrement();
        set(p, e + per);
        p.sendMessage(info.getUsesLeft() == 0
                ? Text.translatable("message.timelordregen.energy.cracked_last")
                : Text.translatable("message.timelordregen.energy.cracked", e + per), true);

        ServerWorld world = p.getServerWorld();
        EnergyNetworking.sendFx(world, p.getPos().add(0, p.getHeight() * 0.6, 0), EnergyFxType.CRACK, p.getId(), 0, 0);
        world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.6f, 1.0f);
        world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    public static void input(ServerPlayerEntity p, EnergyAbility a, boolean pressed) {
        if (a == EnergyAbility.CRACK) {
            if (pressed) crack(p);
            return;
        }

        RegenerationCore info = RegenerationCore.get(p);
        if (info == null) return;

        if (!pressed) {
            EnergyChannel c = info.getChannel();
            if (c != null && (c.ability == a || (a == EnergyAbility.HEAL && c.ability == EnergyAbility.TRANSFER))) {
                stop(p, info, true);
            }
            return;
        }

        if (!p.isAlive() || p.isSpectator()) return;
        if (info.isActive()) {
            p.sendMessage(Text.translatable("message.timelordregen.energy.busy"), true);
            return;
        }

        EnergyAbility ability = a == EnergyAbility.HEAL && p.isSneaking() ? EnergyAbility.TRANSFER : a;
        EnergyChannel cur = info.getChannel();
        if (cur != null && cur.ability == ability) return;
        RegenerationServerConfig c = RegenerationServerConfig.get();
        int need = switch (ability) {
            case HEAL -> c.healCost;
            case TRANSFER -> c.transferAmount;
            default -> 1;
        };
        if (get(p) < Math.max(1, need)) {
            p.sendMessage(emptyMsg(), true);
            return;
        }

        stop(p, info, false);
        if (ability == EnergyAbility.BLAST) p.setSprinting(false);
        info.setChannel(new EnergyChannel(ability));
    }

    public static void stop(ServerPlayerEntity p, RegenerationCore info, boolean released) {
        EnergyChannel c = info.getChannel();
        if (c == null) return;
        info.setChannel(null);
        c.stop(p, released);
    }

    public static Text emptyMsg() {
        return Text.translatable("message.timelordregen.energy.empty", Text.keybind("key.timelordregen.energy_crack"));
    }

    public static DamageSource source(ServerPlayerEntity attacker) {
        RegistryKey<DamageType> type = RegenerationServerConfig.get().ignoresArmor ? DAMAGE : DAMAGE_ARMORED;
        return new DamageSource(attacker.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(type), attacker);
    }

    private static void stop(ServerPlayerEntity p, boolean released) {
        RegenerationCore info = RegenerationCore.get(p);
        if (info != null) stop(p, info, released);
    }

    private static EnergyChannel channel(ServerPlayerEntity p) {
        RegenerationCore info = RegenerationCore.get(p);
        return info == null ? null : info.getChannel();
    }
}
