package dev.amble.ars.compat.origin;

import dev.amble.ars.RegenerationMod;
import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.data.Attachments;
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

    public static void setupRegenerationPower(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) return;
        if (!isTimelordOrigin(player)) return;

        if (player instanceof RegenerationCapable capable) {
            capable.setTimelord(true);

            RegenerationCore info = capable.getRegenerationInfo();
            if (info == null) {
                info = new RegenerationCore();
                info.setUsesLeft(REGEN_ORIGIN_COUNT);
                player.setAttached(Attachments.REGENERATION, info);
            } else {
                info.setUsesLeft(REGEN_ORIGIN_COUNT);
            }

            RegenerationMod.LOGGER.debug("Granted timelord status via Origins to {}", player.getName().getString());
        }
    }

    private static boolean isTimelordOrigin(PlayerEntity player) {
        OriginComponent component = ModComponents.ORIGIN.get(player);
        if (component == null) return false;

        Origin current = component.getOrigin(getDefaultLayer());
        return current != null && !current.equals(Origin.EMPTY)
                && current.getIdentifier().equals(TIMELORD_ORIGIN_ID);
    }

    public static OriginLayer getDefaultLayer() {
        if (DEFAULT_LAYER == null) {
            DEFAULT_LAYER = OriginLayers.getLayer(DEFAULT_LAYER_ID);
            if (DEFAULT_LAYER == null) {
                RegenerationMod.LOGGER.error("Default origin layer not found!");
            }
        }
        return DEFAULT_LAYER;
    }
}