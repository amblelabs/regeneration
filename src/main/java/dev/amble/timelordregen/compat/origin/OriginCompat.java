package dev.amble.timelordregen.compat.origin;

import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.data.Attachments;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class OriginCompat {
    private static final Identifier DEFAULT_LAYER_ID = Identifier.of(Origins.MODID, "origin");
    public static final Identifier TIMELORD_ORIGIN_ID = Identifier.of(RegenerationMod.MOD_ID, "timelord");

    public static void init() {
        RegenerationMod.LOGGER.info("Origins detected, loading compatibility features.");
    }

    public static void setupRegenerationPower(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(player instanceof RegenerationCapable capable)) return;

        OriginComponent component = ModComponents.ORIGIN.get(player);
        if (component == null) return;

        Origin currentOrigin = component.getOrigin(OriginLayers.getLayer(DEFAULT_LAYER_ID));
        boolean isTimelordOrigin = currentOrigin != null && currentOrigin.getIdentifier().equals(TIMELORD_ORIGIN_ID);

        if (isTimelordOrigin) {

            if (!capable.isTimelord()) {
                capable.setTimelord(true);
            }

            RegenerationCore info = capable.getRegenerationInfo();
            if (info == null) {
                info = new RegenerationCore();
                player.setAttached(Attachments.REGENERATION, info);
            }
            info.setUsesLeft(RegenerationCore.MAX_REGENERATIONS);

            RegenerationMod.LOGGER.debug("Origins: granted timelord to {}", player.getName().getString());

        } else if (capable.isTimelord()) {

            RegenerationCore info = capable.getRegenerationInfo();
            if (info != null) {
                info.stopRegeneration(serverPlayer);
                info.resetSkinToBase(serverPlayer);
            }

            capable.setTimelord(false);
            player.setAttached(Attachments.IS_TIMELORD, false);

            RegenerationMod.LOGGER.debug("Origins: revoked timelord from {}", player.getName().getString());
        }
    }
}