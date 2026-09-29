package dev.amble.timelordregen.compat.ait;

import dev.amble.timelordregen.core.RegenerationExplosion;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.api.RegenerationEvents;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.core.animation.AnimationTemplate;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.handler.travel.TravelUtil;
import dev.amble.ait.core.tardis.control.impl.pos.IncrementManager;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.ait.registry.impl.DesktopRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AITCompat implements TardisCompatBridge {

    public static void init() {
        RegenerationMod.LOGGER.info("AIT detected, loading compatibility features.");

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
    public boolean isTardis(World world) {
        return TardisServerWorld.isTardisDimension(world);
    }

    @Override
    public boolean drawFuel(World world, double amount) {
        if (amount <= 0.0 || !(world instanceof TardisServerWorld tw)) return true;
        ServerTardis tardis = tw.getTardis();
        if (tardis == null || tardis.getFuel() < amount) return false;
        tardis.removeFuel(amount);
        return true;
    }

    @Override
    public void tickRegenerationOverload(LivingEntity entity, Vec3d center) {
        if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;
        if (!(entity.getWorld() instanceof ServerWorld world)) return;

        ServerTardis tardis = ((TardisServerWorld) entity.getWorld()).getTardis();
        if (tardis == null) return;

        TravelUtil.randomPos(tardis, 50000, IncrementManager.increment(tardis), cached -> {
            world.getServer().execute(() -> {
                tardis.travel().destination(cached);
                tardis.removeFuel(0.1d * IncrementManager.increment(tardis) * tardis.travel().instability());
            });
        });
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