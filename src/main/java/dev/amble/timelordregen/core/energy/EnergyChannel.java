package dev.amble.timelordregen.core.energy;

import dev.amble.timelordregen.config.RegenerationServerConfig;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.data.tree.RegenerationSounds;
import dev.amble.timelordregen.network.EnergyNetworking;
import dev.amble.lib.animation.AnimatedEntity;
import dev.amble.lib.animation.AnimationTracker;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Clearable;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EnergyChannel {
    private static final String ANIM = "regen_energy";
    private static final int MAX_FX = 6;
    private static final Object2LongOpenHashMap<UUID> CHAIN_READY = new Object2LongOpenHashMap<>();

    final EnergyAbility ability;
    private final Map<BlockPos, Crack> cracks = new HashMap<>();
    private ServerWorld world;
    private int casterId;
    int phase;
    private int ticks;
    int targetId = -1;
    private int slots;
    private int broke;
    private int fx;
    private boolean done;

    public EnergyChannel(EnergyAbility ability) {
        this.ability = ability;
        this.phase = ability == EnergyAbility.BLAST ? EnergyFxType.CHARGE : EnergyFxType.STREAM;
    }

    public static void forget(ServerPlayerEntity player) {
        CHAIN_READY.removeLong(player.getUuid());
    }

    public boolean tick(ServerPlayerEntity caster, RegenerationCore info) {
        if (done) return false;

        if (world == null) {
            world = caster.getServerWorld();
            casterId = caster.getId();
            EnergyNetworking.sendChannel(caster, ability, phase, targetId);
            if (ability == EnergyAbility.BLAST) play(caster, "energy_charge");
        }

        if (!caster.isAlive() || caster.isSpectator() || info.isActive() || caster.getServerWorld() != world) {
            stop(caster, false);
            return false;
        }

        boolean ok = switch (ability) {
            case BLAST -> blast(caster);
            case HEAL -> heal(caster);
            case TRANSFER -> transfer(caster);
            default -> false;
        };

        if (!ok) {
            stop(caster, true);
            return false;
        }
        return true;
    }

    public void stop(ServerPlayerEntity caster, boolean released) {
        if (done) return;
        done = true;

        if (world != null) {
            for (Map.Entry<BlockPos, Crack> e : cracks.entrySet()) {
                if (e.getValue().slot >= 0) world.setBlockBreakingInfo(fakeId(e.getValue().slot), e.getKey(), -1);
            }
        }
        cracks.clear();
        slots = 0;

        if (released && phase == EnergyFxType.STREAM && ability == EnergyAbility.BLAST) {
            burst(caster);
        } else if (!released || ability != EnergyAbility.BLAST || !chain(caster)) {
            halt(caster, null);
        }

        EnergyNetworking.sendChannel(caster, ability, EnergyFxType.STOP, -1);
    }

    private boolean blast(ServerPlayerEntity caster) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        if (phase == EnergyFxType.CHARGE) {
            if (++ticks < c.chargeTicks) return true;
            phase = EnergyFxType.STREAM;
            ticks = 0;
            EnergyNetworking.sendChannel(caster, ability, phase, -1);
            play(caster, "energy_stream");
        }

        if (!RegenEnergy.spend(caster, c.beamDrain)) {
            empty(caster);
            return false;
        }
        ticks++;
        broke = 0;
        fx = 0;

        Vec3d eye = caster.getEyePos();
        Vec3d dir = caster.getRotationVec(1.0f);
        BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(c.beamRange)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster));
        Vec3d end = hit.getPos();
        double len = Math.sqrt(eye.squaredDistanceTo(end));
        boolean safe = RegenEnergy.safe(world);

        for (Entity e : safe ? List.<Entity>of() : world.getOtherEntities(caster, new Box(eye, end).expand(c.beamRadius + c.beamSpread * len), e -> hittable(caster, e))) {
            Vec3d at = e.getBoundingBox().getCenter();
            double t = MathHelper.clamp(at.subtract(eye).dotProduct(dir), 0.0, len);
            double r = c.beamRadius + c.beamSpread * t;
            if (at.squaredDistanceTo(eye.add(dir.multiply(t))) > r * r) continue;

            Vec3d v0 = e.getVelocity();
            RegenEnergy.hurt(caster, e, c.beamDamage);
            if (!pushable(caster, e)) continue;
            double k = 0.3 * c.knockback;
            Vec3d v = v0.add(dir.x * k, dir.y * k + 0.12 * c.knockback, dir.z * k);
            e.setVelocity(v.x, Math.min(v.y, 0.6 * Math.max(1.0f, c.knockback)), v.z);
            e.velocityModified = true;
        }

        boolean breaks = !safe && world.getGameRules().getBoolean(RegenEnergy.DESTROYS_BLOCKS);
        long now = world.getTime();

        if (breaks && hit.getType() == HitResult.Type.BLOCK) {
            sphere(caster, end, c.beamBreakRadius, c.beamBreakPower, now);
        }

        if (ticks % Math.max(1, c.impactTicks) == 0) {
            impact(caster, end, safe, breaks, now);
        }

        if (ticks % 10 == 0) {
            sweep(now);
        }
        return true;
    }

    private void impact(ServerPlayerEntity caster, Vec3d end, boolean safe, boolean breaks, long now) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        EnergyNetworking.sendFx(world, end, EnergyFxType.IMPACT, caster.getId(), (float) c.impactRadius, 0.0f);

        for (Entity e : safe ? List.<Entity>of() : world.getOtherEntities(caster, new Box(end, end).expand(c.impactRadius), e -> hittable(caster, e))) {
            if (e.squaredDistanceTo(end) > c.impactRadius * c.impactRadius) continue;
            RegenEnergy.hurt(caster, e, c.impactDamage);
            knock(caster, e, end.x, end.z, 3.0 * c.knockback, 0.6 * c.knockback);
        }

        if (breaks) {
            sphere(caster, end, c.impactBreakRadius, c.impactBreakPower, now);
            ignite(caster, end, c.fires, Math.max(1.0, c.impactBreakRadius * 0.7));
        }

        world.playSound(null, end.x, end.y, end.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.3f + world.getRandom().nextFloat() * 0.2f);
        world.playSound(null, end.x, end.y, end.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private void sphere(ServerPlayerEntity caster, Vec3d c, double r, float amount, long now) {
        if (r <= 0.0 || amount <= 0.0f) return;
        double r2 = r * r;
        for (BlockPos p : BlockPos.iterate(MathHelper.floor(c.x - r), MathHelper.floor(c.y - r), MathHelper.floor(c.z - r),
                MathHelper.floor(c.x + r), MathHelper.floor(c.y + r), MathHelper.floor(c.z + r))) {
            if (p.getSquaredDistanceFromCenter(c.x, c.y, c.z) <= r2) feed(caster, p, amount, now);
        }
    }

    private void feed(ServerPlayerEntity caster, BlockPos pos, float amount, long now) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        BlockState s = world.getBlockState(pos);
        float h = s.getHardness(world, pos);
        Crack k = cracks.get(pos);

        if (h < 0 || h > c.maxHardness || !breakable(caster, pos, s)) {
            if (k != null) forget(k);
            return;
        }

        if (k == null) {
            k = new Crack(pos.toImmutable(), slot());
            cracks.put(k.pos, k);
        }
        k.seen = now;
        k.p += amount / Math.max(0.3f, h);

        if (k.p >= 1.0f && broke < c.maxBreaks) {
            forget(k);
            annihilate(caster, k.pos, s);
            return;
        }

        int stage = Math.min(9, (int) (k.p * 10.0f));
        if (k.slot >= 0 && stage != k.stage) {
            k.stage = stage;
            world.setBlockBreakingInfo(fakeId(k.slot), k.pos, stage);
        }
    }

    private void annihilate(ServerPlayerEntity caster, BlockPos pos, BlockState s) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, caster, pos, s, be)) return;
        broke++;
        if (be instanceof LootableContainerBlockEntity l) l.setLootTable(null, 0L);
        if (be != null) Clearable.clear(be);
        BlockPos other = other(pos, s);
        if (other != null && world.getBlockState(other).isOf(s.getBlock()))
            world.setBlockState(other, world.getFluidState(other).getBlockState(), Block.NOTIFY_ALL | Block.FORCE_STATE);
        world.setBlockState(pos, world.getFluidState(pos).getBlockState(), Block.NOTIFY_ALL);
        world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(caster, s));
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(world, caster, pos, s, be);
        if (fx++ >= MAX_FX) return;
        EnergyNetworking.sendFx(world, pos.toCenterPos(), EnergyFxType.DISSOLVE, caster.getId(), 0.0f, 0.0f);
        world.playSound(null, pos, s.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 0.8f, 0.7f + world.getRandom().nextFloat() * 0.3f);
    }

    private void ignite(ServerPlayerEntity caster, Vec3d c, int n, double spread) {
        if (!caster.canModifyBlocks()) return;
        Random r = world.getRandom();
        for (int i = 0; i < n; i++) {
            BlockPos p = BlockPos.ofFloored(c.x + r.nextGaussian() * spread, c.y + r.nextGaussian() * 1.2, c.z + r.nextGaussian() * spread);
            if (!world.getBlockState(p).isAir() || !world.canPlayerModifyAt(caster, p)) continue;
            if (!AbstractFireBlock.canPlaceAt(world, p, Direction.UP)) continue;
            world.setBlockState(p, AbstractFireBlock.getState(world, p), Block.NOTIFY_ALL);
        }
    }

    private static BlockPos other(BlockPos pos, BlockState s) {
        if (s.contains(Properties.DOUBLE_BLOCK_HALF))
            return s.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
        if (s.getBlock() instanceof BedBlock) return pos.offset(BedBlock.getOppositePartDirection(s));
        return null;
    }

    private boolean breakable(ServerPlayerEntity caster, BlockPos pos, BlockState s) {
        if (s.isAir() || s.getBlock() instanceof FluidBlock) return false;
        if (s.isIn(RegenEnergy.IMMUNE)) return false;
        if (!world.canPlayerModifyAt(caster, pos)) return false;
        return !caster.isBlockBreakingRestricted(world, pos, caster.interactionManager.getGameMode());
    }

    private void forget(Crack k) {
        cracks.remove(k.pos);
        if (k.slot < 0) return;
        slots &= ~(1 << k.slot);
        world.setBlockBreakingInfo(fakeId(k.slot), k.pos, -1);
    }

    private void sweep(long now) {
        Iterator<Map.Entry<BlockPos, Crack>> it = cracks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Crack> e = it.next();
            Crack k = e.getValue();
            if (now - k.seen <= 40) continue;
            it.remove();
            if (k.slot < 0) continue;
            slots &= ~(1 << k.slot);
            world.setBlockBreakingInfo(fakeId(k.slot), e.getKey(), -1);
        }
    }

    private int slot() {
        for (int i = 0; i < 16; i++) {
            if ((slots & (1 << i)) == 0) {
                slots |= 1 << i;
                return i;
            }
        }
        return -1;
    }

    private int fakeId(int slot) {
        return -1000 - casterId * 16 - slot;
    }

    private boolean heal(ServerPlayerEntity caster) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        if (++ticks % Math.max(1, c.healTicks) != 0) return true;
        if (RegenEnergy.get(caster) < Math.max(1, c.healCost)) {
            empty(caster);
            return false;
        }

        if (caster.getHealth() < caster.getMaxHealth()) {
            if (!RegenEnergy.spend(caster, c.healCost)) return false;
            caster.heal(c.healAmount);
        } else if (caster.getAbsorptionAmount() < c.maxAbsorption) {
            if (!RegenEnergy.spend(caster, c.healCost)) return false;
            caster.setAbsorptionAmount(Math.min(c.maxAbsorption, caster.getAbsorptionAmount() + c.healAmount));
        }
        return true;
    }

    private boolean transfer(ServerPlayerEntity caster) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        if (++ticks % Math.max(1, c.transferTicks) != 0) return true;
        if (RegenEnergy.get(caster) < c.transferAmount) {
            empty(caster);
            return false;
        }

        Vec3d eye = caster.getEyePos();
        Vec3d dir = caster.getRotationVec(1.0f);
        Vec3d end = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(c.transferRange)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster)).getPos();
        EntityHitResult hit = ProjectileUtil.raycast(caster, eye, end, caster.getBoundingBox().stretch(end.subtract(eye)).expand(1.0),
                e -> e instanceof ServerPlayerEntity && e.isAlive() && !e.isSpectator(), eye.squaredDistanceTo(end));
        ServerPlayerEntity target = hit != null ? (ServerPlayerEntity) hit.getEntity() : null;

        int id = target != null ? target.getId() : -1;
        if (id != targetId) {
            targetId = id;
            EnergyNetworking.sendChannel(caster, ability, phase, id);
        }
        if (target == null) return true;

        RegenerationCore other = RegenerationCore.get(target);
        if (other == null) {
            if (target.getHealth() >= target.getMaxHealth()) return true;
            if (!RegenEnergy.spend(caster, c.transferAmount)) return false;
            target.heal(c.transferHeal);
            EnergyNetworking.sendFx(world, caster.getBoundingBox().getCenter(), EnergyFxType.TRANSFER, target.getId(), 0.0f, 0.0f);
            return true;
        }

        float eff = Math.max(0.0f, c.transferEfficiency);
        int give = Math.round(c.transferAmount * eff);
        int has = RegenEnergy.get(target);
        int next = has + give;
        boolean crack = false;
        boolean full = false;
        if (next > c.energyCap) {
            if (c.transferGrantsRegens && other.getUsesLeft() < RegenerationCore.MAX_REGENERATIONS) {
                next -= c.energyPerRegen;
                crack = true;
            } else {
                next = c.energyCap;
                full = true;
            }
        }
        int cost = full ? (eff <= 0.0f ? 0 : Math.min(c.transferAmount, MathHelper.ceil(Math.max(0, next - has) / eff))) : c.transferAmount;
        if (cost > 0 || crack || next != has) {
            if (!RegenEnergy.spend(caster, cost)) return false;
            if (crack) other.setUsesLeft(other.getUsesLeft() + 1);
            RegenEnergy.set(target, next);
        }
        if (full) {
            caster.sendMessage(Text.translatable("message.timelordregen.energy.transfer_full", target.getDisplayName()), true);
            if (next == has) return false;
        }

        EnergyNetworking.sendFx(world, caster.getBoundingBox().getCenter(), EnergyFxType.TRANSFER, target.getId(), 0.0f, 0.0f);
        return !full;
    }

    private void burst(ServerPlayerEntity caster) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        ServerWorld w = caster.getServerWorld();
        Vec3d at = caster.getPos();
        double rr = c.releaseRadius;
        EnergyNetworking.sendFx(w, at, EnergyFxType.RELEASE, caster.getId(), (float) rr, 0.0f);

        boolean safe = RegenEnergy.safe(w);
        for (Entity e : safe || rr <= 0.0 ? List.<Entity>of() : w.getOtherEntities(caster, caster.getBoundingBox().expand(rr), e -> hittable(caster, e))) {
            double d = Math.sqrt(e.squaredDistanceTo(caster));
            if (d > rr) continue;
            RegenEnergy.hurt(caster, e, c.releaseDamage * (float) (1.0 - d / rr * 0.5));
            knock(caster, e, caster.getX(), caster.getZ(), (1.5 + 3.0 * (1.0 - d / rr)) * c.knockback, 0.5 * c.knockback);
        }

        double r = c.releaseBreakRadius;
        if (w == world && !safe && r > 0.0 && w.getGameRules().getBoolean(RegenEnergy.DESTROYS_BLOCKS)) {
            broke = 0;
            fx = 0;
            BlockPos feet = caster.getBlockPos();
            int ri = MathHelper.ceil(r);
            for (BlockPos p : BlockPos.iterate(feet.add(-ri, 0, -ri), feet.add(ri, 5, ri))) {
                double d2 = p.getSquaredDistanceFromCenter(at.x, at.y, at.z);
                if (d2 < 2.25 || d2 > r * r || broke >= c.maxBreaks * 2) continue;
                BlockState s = w.getBlockState(p);
                float h = s.getHardness(w, p);
                if (h < 0.0f || h > c.releaseBreakHardness) continue;
                BlockPos q = p.toImmutable();
                if (breakable(caster, q, s)) annihilate(caster, q, s);
            }
        }

        w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0f, 1.0f);
        w.playSound(null, at.x, at.y, at.z, RegenerationSounds.ELEVEN_REGEN_END, SoundCategory.PLAYERS, 0.9f, 1.25f);
        play(caster, "energy_release");
        Scheduler.get().runTaskLater(() -> halt(caster, "energy_release"), TaskStage.END_SERVER_TICK, TimeUnit.TICKS, 20);
    }

    private boolean chain(ServerPlayerEntity caster) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        if (!c.chainEnabled) return false;
        ServerWorld w = caster.getServerWorld();
        long now = w.getTime();
        long ready = CHAIN_READY.getLong(caster.getUuid());
        if (now < ready) {
            caster.sendMessage(Text.translatable("message.timelordregen.energy.chain_cooldown", MathHelper.ceil((ready - now) / 20.0f)), true);
            return false;
        }
        LivingEntity first = pick(caster, w, c.chainRange, c.chainAimAssist);
        if (first == null) return false;
        int max = Math.min(c.chainTargets, c.chainCost > 0 ? RegenEnergy.get(caster) / c.chainCost : c.chainTargets);
        if (max < 1) {
            empty(caster);
            return false;
        }
        world = w;
        casterId = caster.getId();

        List<LivingEntity> nodes = new ArrayList<>();
        nodes.add(first);
        Vec3d o = first.getPos();
        double outer = c.chainRadius * c.chainRadius, hop = c.chainHop * c.chainHop;
        List<LivingEntity> pool = w.getEntitiesByClass(LivingEntity.class, first.getBoundingBox().expand(c.chainRadius),
                e -> e != first && chainable(caster, e) && e.squaredDistanceTo(o) <= outer);
        LivingEntity cur = first;
        while (nodes.size() < max && !pool.isEmpty()) {
            int best = -1;
            double bd = hop;
            for (int i = 0; i < pool.size(); i++) {
                double d = pool.get(i).squaredDistanceTo(cur);
                if (d <= bd) {
                    bd = d;
                    best = i;
                }
            }
            if (best < 0) break;
            cur = pool.get(best);
            pool.set(best, pool.get(pool.size() - 1));
            pool.remove(pool.size() - 1);
            nodes.add(cur);
        }

        int step = Math.max(1, c.chainHopTicks);
        RegenEnergy.spend(caster, nodes.size() * c.chainCost);
        CHAIN_READY.put(caster.getUuid(), now + c.chainCooldown);
        EnergyNetworking.sendChain(w, caster, nodes, step);
        for (int i = 0; i < nodes.size(); i++) {
            LivingEntity e = nodes.get(i);
            Entity from = i == 0 ? caster : nodes.get(i - 1);
            Scheduler.get().runTaskLater(() -> hop(caster, w, e, from), TaskStage.END_SERVER_TICK, TimeUnit.TICKS, 1 + i * step);
        }

        w.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7f, 1.5f);
        w.playSound(null, caster.getX(), caster.getY(), caster.getZ(), RegenerationSounds.ELEVEN_REGEN_END, SoundCategory.PLAYERS, 0.9f, 1.4f);
        play(caster, "energy_release");
        Scheduler.get().runTaskLater(() -> halt(caster, "energy_release"), TaskStage.END_SERVER_TICK, TimeUnit.TICKS, 20);
        return true;
    }

    private void hop(ServerPlayerEntity caster, ServerWorld w, LivingEntity e, Entity from) {
        if (!e.isAlive() || e.getWorld() != w) return;
        RegenerationServerConfig c = RegenerationServerConfig.get();
        Vec3d at = e.getBoundingBox().getCenter();
        double sr = c.chainSplashRadius;
        EnergyNetworking.sendFx(w, at, EnergyFxType.CHAIN, e.getId(), (float) Math.max(1.0, sr), 0.0f);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1.1f + w.getRandom().nextFloat() * 0.3f);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.2f, 1.4f);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.7f, 1.5f);
        if (RegenEnergy.safe(w)) return;

        RegenEnergy.hurt(caster, e, c.chainDamage);
        knock(caster, e, from.getX(), from.getZ(), 2.5 * c.knockback, 0.7 * c.knockback);
        if (sr > 0.0) {
            for (Entity o : w.getOtherEntities(caster, e.getBoundingBox().expand(sr), x -> x != e && chainable(caster, x))) {
                if (o.squaredDistanceTo(at) > sr * sr) continue;
                RegenEnergy.hurt(caster, o, c.chainSplashDamage);
                knock(caster, o, at.x, at.z, 1.8 * c.knockback, 0.5 * c.knockback);
            }
        }

        if (w.getGameRules().getBoolean(RegenEnergy.DESTROYS_BLOCKS)) {
            broke = 0;
            fx = 0;
            crater(caster, at, c.chainCraterRadius, c.chainCraterHardness, c.maxBreaks);
            if (c.chainCraterRadius > 0.0) ignite(caster, at, c.fires, Math.max(1.0, c.chainCraterRadius));
        }
    }

    private void crater(ServerPlayerEntity caster, Vec3d c, double r, float hardness, int max) {
        if (r <= 0.0) return;
        double r2 = r * r;
        for (BlockPos p : BlockPos.iterate(MathHelper.floor(c.x - r), MathHelper.floor(c.y - r), MathHelper.floor(c.z - r),
                MathHelper.floor(c.x + r), MathHelper.floor(c.y + r), MathHelper.floor(c.z + r))) {
            if (broke >= max) return;
            if (p.getSquaredDistanceFromCenter(c.x, c.y, c.z) > r2) continue;
            BlockState s = world.getBlockState(p);
            float h = s.getHardness(world, p);
            if (h < 0.0f || h > hardness) continue;
            BlockPos q = p.toImmutable();
            if (breakable(caster, q, s)) annihilate(caster, q, s);
        }
    }

    private static LivingEntity pick(ServerPlayerEntity caster, ServerWorld w, double range, double assist) {
        Vec3d eye = caster.getEyePos();
        Vec3d end = w.raycast(new RaycastContext(eye, eye.add(caster.getRotationVec(1.0f).multiply(range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster)).getPos();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : w.getOtherEntities(caster, new Box(eye, end).expand(assist + 0.5), x -> chainable(caster, x))) {
            Optional<Vec3d> at = e.getBoundingBox().expand(assist).raycast(eye, end);
            if (at.isEmpty()) continue;
            double d = eye.squaredDistanceTo(at.get());
            if (d < bd) {
                bd = d;
                best = (LivingEntity) e;
            }
        }
        return best;
    }

    private static boolean chainable(ServerPlayerEntity caster, Entity e) {
        return e != caster && !(e instanceof ArmorStandEntity) && hittable(caster, e) && pushable(caster, e);
    }

    private static void knock(ServerPlayerEntity caster, Entity e, double x, double z, double str, double up) {
        if (!e.isAlive() || !pushable(caster, e) || str <= 0.0) return;
        ((LivingEntity) e).takeKnockback(str, x - e.getX(), z - e.getZ());
        e.addVelocity(0.0, up, 0.0);
        e.velocityModified = true;
    }

    private static void empty(ServerPlayerEntity caster) {
        caster.sendMessage(RegenEnergy.emptyMsg(), true);
    }

    private static boolean hittable(ServerPlayerEntity caster, Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || e.isSpectator() || caster.isConnectedThroughVehicle(e)) return false;
        return !(e instanceof Tameable t) || !caster.getUuid().equals(t.getOwnerUuid());
    }

    private static boolean pushable(ServerPlayerEntity caster, Entity e) {
        if (!(e instanceof PlayerEntity p)) return true;
        return !p.getAbilities().invulnerable && caster.shouldDamagePlayer(p);
    }

    private static void play(ServerPlayerEntity caster, String name) {
        if (caster instanceof AnimatedEntity animated) {
            animated.playAnimation(BedrockAnimationReference.parse(new Identifier(ANIM, name)));
        }
    }

    private static void halt(ServerPlayerEntity caster, String only) {
        if (!(caster instanceof AnimatedEntity animated)) return;
        BedrockAnimationReference cur = animated.getCurrentAnimation();
        if (cur == null || !ANIM.equals(cur.fileName())) return;
        if (only != null && !only.equals(cur.animationName())) return;
        animated.getAnimationState().stop();
        AnimationTracker.getInstance().remove(animated.getUuid());
    }

    private static final class Crack {
        final BlockPos pos;
        final int slot;
        float p;
        int stage = -1;
        long seen;

        Crack(BlockPos pos, int slot) {
            this.pos = pos;
            this.slot = slot;
        }
    }
}
