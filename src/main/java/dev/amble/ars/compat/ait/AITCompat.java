package dev.amble.ars.compat.ait;

import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.ait.registry.impl.DesktopRegistry;
import dev.amble.ars.RegenerationMod;
import dev.amble.ars.api.RegenerationEvents;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.core.animation.AnimationTemplate;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class AITCompat {
    public static void init() {
        RegenerationMod.LOGGER.info("AIT detected, loading compatibility features.");

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

    private static void withTardis(Entity entity, Consumer<ServerTardis> action) {
        if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;
        ServerTardis tardis = ((TardisServerWorld) entity.getWorld()).getTardis();
        if (tardis == null) return;
        action.accept(tardis);
    }

    private static void triggerAlarm(ServerTardis tardis, Entity entity) {
        tardis.alarm().enable(Text.translatable("timelordregen.tardis.alarm_message", entity.getEntityName()));
    }
}