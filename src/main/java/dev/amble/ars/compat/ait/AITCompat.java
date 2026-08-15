package dev.amble.ars.compat.ait;

import dev.amble.ars.core.RegenerationExplosion;
import dev.amble.ars.RegenerationMod;
import dev.amble.ars.api.RegenerationEvents;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.core.animation.AnimationTemplate;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.handler.travel.TravelUtil;
import dev.amble.ait.core.tardis.control.impl.pos.IncrementManager;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.ait.registry.impl.DesktopRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AITCompat implements TardisCompatBridge {

    public static void init() {
        RegenerationMod.LOGGER.info("AIT detected, loading compatibility features.");

        // 注册桥接到核心模块
        RegenerationExplosion.setTardisBridge(new AITCompat());

        RegenerationEvents.START.register((entity, data) ->
                withTardis(entity, tardis -> triggerAlarm(tardis, entity))
        );

        RegenerationEvents.CHANGE_STAGE.register((entity, data, stage) ->
                withTardis(entity, tardis -> {
                    triggerAlarm(tardis, entity);
                    if (stage == AnimationTemplate.Stage.LOOP && tardis.travel().inFlight()) {
                        tardis.travel().crash();
                    }
                })
        );

        RegenerationEvents.FINISH.register((entity, data) ->
                withTardis(entity, tardis -> {
                    int mode = data.getTardisInteriorMode();
                    if (mode == RegenerationCore.TARDIS_MODE_DISABLED) return;

                    if (mode == RegenerationCore.TARDIS_MODE_ENABLED) {
                        tardis.interiorChanging().queueInteriorChange(DesktopRegistry.getInstance().getRandom(tardis));
                    } else if (mode == RegenerationCore.TARDIS_MODE_REFURBISH) {
                        tardis.interiorChanging().queueInteriorChange(null);
                    }
                })
        );
    }

    @Override
    public void tickRegenerationOverload(LivingEntity entity, Vec3d center) {
        if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;
        if (!(entity.getWorld() instanceof ServerWorld world)) return;

        ServerTardis tardis = ((TardisServerWorld) entity.getWorld()).getTardis();
        if (tardis == null) return;

        for (BlockPos consolePos : tardis.getDesktop().getConsolePos()) {
            spawnOverloadParticles(world, consolePos);

            float pitch = 0.5f + world.random.nextFloat() * 1.0f;
            world.playSound(null, consolePos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 6f, pitch);
        }

        // 50000 随机坐标 + 切回主线程防并发崩溃
        TravelUtil.randomPos(tardis, 50000, IncrementManager.increment(tardis), cached -> {
            world.getServer().execute(() -> {
                tardis.travel().destination(cached);
                tardis.removeFuel(0.1d * IncrementManager.increment(tardis) * tardis.travel().instability());
            });
        });
    }

    private static void spawnOverloadParticles(ServerWorld world, BlockPos pos) {
        for (int i = 0; i < 80; i++) {
            double ox = (Math.random() - 0.5) * 4.0;
            double oy = Math.random() * 3.0;
            double oz = (Math.random() - 0.5) * 4.0;
            double x = pos.getX() + 0.5 + ox;
            double y = pos.getY() + 1.5 + oy;
            double z = pos.getZ() + 0.5 + oz;

            world.spawnParticles(ParticleTypes.SNEEZE, x, y, z, 2, 0.5, 0.5, 0.5, 0.8);
            world.spawnParticles(ParticleTypes.ASH, x, y, z, 2, 0.5, 0.5, 0.5, 0.8);
            world.spawnParticles(ParticleTypes.LAVA, x, y, z, 2, 0.5, 0.5, 0.5, 0.8);
            world.spawnParticles(ParticleTypes.SMALL_FLAME, x, y, z, 2, 0.5, 0.5, 0.5, 0.8);
            world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 8, 0.5, 0.5, 0.5, 0.8);
        }
    }

    private static void withTardis(Entity entity, java.util.function.Consumer<ServerTardis> action) {
        if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;
        ServerTardis tardis = ((TardisServerWorld) entity.getWorld()).getTardis();
        if (tardis == null) return;
        action.accept(tardis);
    }

    private static void triggerAlarm(ServerTardis tardis, Entity entity) {
        tardis.alarm().enable(Text.translatable("timelordregen.tardis.alarm_message", entity.getEntityName()));
    }
}