package dev.amble.ars.compat.origin;

import dev.amble.ars.RegenerationMod;
import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.data.Attachments;
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

    /**
     * 当玩家通过 Origins 切换起源时调用。
     * 选了 timelordregen:timelord → 授予时间领主
     * 选了其他起源 → 撤销时间领主（如果当前是）
     */
    public static void setupRegenerationPower(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(player instanceof RegenerationCapable capable)) return;

        OriginComponent component = ModComponents.ORIGIN.get(player);
        if (component == null) return;

        Origin currentOrigin = component.getOrigin(OriginLayers.getLayer(DEFAULT_LAYER_ID));
        boolean isTimelordOrigin = currentOrigin != null && currentOrigin.getIdentifier().equals(TIMELORD_ORIGIN_ID);

        if (isTimelordOrigin) {
            //regen settimelord
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
            //regen detimelord
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