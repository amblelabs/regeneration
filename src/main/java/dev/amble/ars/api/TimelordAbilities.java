package dev.amble.ars.api;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;

public class TimelordAbilities {

    private static final float HUNGER_EXHAUSTION_MULTIPLIER = 0.75f;
    private static final float RESISTANCE_PERCENT = 0.10f;

    public static float modifyExhaustion(float original) {
        return original * HUNGER_EXHAUSTION_MULTIPLIER;
    }

    public static float applyResistance(float original) {
        return original * (1.0f - RESISTANCE_PERCENT);
    }

    public static boolean shouldCompensateAirLoss(int tickCount) {
        return tickCount % 4 != 0;
    }

    public static boolean isImmuneToStarvation(PlayerEntity player, DamageSource source) {
        return source.isOf(DamageTypes.STARVE)
                && player instanceof RegenerationCapable capable
                && capable.isTimelord();
    }
}