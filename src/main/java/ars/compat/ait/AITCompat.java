package ars.compat.ait;

import ars.RegenerationMod;
import ars.core.animation.AnimationTemplate;
import ars.api.RegenerationEvents;
import ars.core.RegenerationCore;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.ait.registry.impl.DesktopRegistry;
import net.minecraft.text.Text;

public class AITCompat {
    public static void init() {
        RegenerationMod.LOGGER.info("AIT detected, loading compatibility features.");

        RegenerationEvents.START.register(((entity, data) -> {
            if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;

            ServerTardis tardis = ((TardisServerWorld) (entity.getWorld())).getTardis();
            if (tardis == null) return;

            tardis.alarm().enable(Text.translatable("timelordregen.tardis.alarm_message", entity.getEntityName()));
        }));

        RegenerationEvents.CHANGE_STAGE.register(((entity, data, stage) -> {
            if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;

            ServerTardis tardis = ((TardisServerWorld) (entity.getWorld())).getTardis();
            if (tardis == null) return;

            tardis.alarm().enable(Text.translatable("timelordregen.tardis.alarm_message", entity.getEntityName()));

            if (stage == AnimationTemplate.Stage.LOOP && tardis.travel().inFlight()) {
                tardis.travel().crash();
            }
        }));

        RegenerationEvents.FINISH.register((entity, data) -> {
            if (!TardisServerWorld.isTardisDimension(entity.getWorld())) return;

            ServerTardis tardis = ((TardisServerWorld) (entity.getWorld())).getTardis();
            if (tardis == null) return;

            int mode = data.getTardisInteriorMode();

            // 模式 1：关闭，什么都不做
            if (mode == RegenerationCore.TARDIS_MODE_DISABLED) return;

            if (mode == RegenerationCore.TARDIS_MODE_ENABLED) {
                // 默认：随机更换内饰类型
                tardis.interiorChangingHandler().queueInteriorChange(DesktopRegistry.getInstance().getRandom(tardis));
            } else if (mode == RegenerationCore.TARDIS_MODE_REFURBISH) {
                // 只重构：触发内饰重新生成，但不更换内饰类型
                // 传 null 表示"不指定新类型"，AIT 内部会使用当前类型重新生成
                tardis.interiorChangingHandler().queueInteriorChange(null);
            }
        });
    }
}