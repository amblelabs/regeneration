package ars.compat.origin;

import ars.RegenerationMod;
import ars.api.RegenerationCapable;
import ars.core.RegenerationCore;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class OriginCompat {
    private static OriginLayer DEFAULT_LAYER;
    private static final Identifier DEFAULT_LAYER_ID = Identifier.of(Origins.MODID, "origin");
    public static final Identifier TIMELORD_ORIGIN_ID = Identifier.of(RegenerationMod.MOD_ID, "timelord");
    public static final int REGEN_ORIGIN_COUNT = 12;

    public static void init() {
        RegenerationMod.LOGGER.info("Origins detected, loading compatibility features.");
    }

    /**
     * 当玩家通过 Origins 选择起源时调用。
     * 只有选了 timelordregen:timelord 起源才设为时间领主。
     */
    public static void setupRegenerationPower(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        OriginComponent component = ModComponents.ORIGIN.get(serverPlayer);
        if (component == null) return;

        Origin currentOrigin = component.getOrigin(getDefaultLayer());
        if (currentOrigin == null || currentOrigin.equals(Origin.EMPTY)) return;

        // 精确匹配：只有 timelordregen:timelord 才触发
        if (!currentOrigin.getIdentifier().equals(TIMELORD_ORIGIN_ID)) return;

        RegenerationMod.LOGGER.debug("Setting up regeneration power for player {}", player.getName().getString());

        if (player instanceof RegenerationCapable capable) {
            capable.setTimelord(true);
            RegenerationCore info = capable.getRegenerationInfo();
            if (info != null) {
                info.setUsesLeft(REGEN_ORIGIN_COUNT);
            }
        }
    }

    public static OriginLayer getDefaultLayer() {
        if (DEFAULT_LAYER == null) {
            DEFAULT_LAYER = OriginLayers.getLayer(DEFAULT_LAYER_ID);
            if (DEFAULT_LAYER == null) {
                RegenerationMod.LOGGER.error("Default origin layer not found, Origins compatibility may not work correctly.");
            }
        }
        return DEFAULT_LAYER;
    }
}