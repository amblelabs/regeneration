package dev.amble.ars.mixin.TimelordAbilities;

import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.api.TimelordAbilities;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityAbilitiesMixin {

    @Unique
    private int timelord$regenBoostTimer = 0;

    @Unique
    private boolean timelord$isTimelord() {
        PlayerEntity player = (PlayerEntity)(Object) this;
        return player instanceof RegenerationCapable capable && capable.isTimelord();
    }

    // ========== 饥饿消耗 -25% ==========
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float timelord$modifyExhaustion(float exhaustion) {
        if (timelord$isTimelord()) {
            return TimelordAbilities.modifyExhaustion(exhaustion);
        }
        return exhaustion;
    }

    // ========== 常驻 10% 抗性 ==========
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float timelord$applyResistance(float amount, DamageSource source) {
        if (timelord$isTimelord()) {
            RegenerationCore info = ((RegenerationCapable)(Object) this).getRegenerationInfo();
            if (info != null && !info.isInvulnerable()) {
                return TimelordAbilities.applyResistance(amount);
            }
        }
        return amount;
    }

    // ========== 饥饿伤害免疫 ==========
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void timelord$cancelStarveDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (TimelordAbilities.isImmuneToStarvation((PlayerEntity)(Object) this, source)) {
            cir.setReturnValue(false);
        }
    }

    // ========== 生命恢复 1.5x + 氧气消耗 75% ==========
    @Inject(method = "tick", at = @At("TAIL"))
    private void timelord$tickAbilities(CallbackInfo ci) {
        if (!timelord$isTimelord()) {
            this.timelord$regenBoostTimer = 0;
            return;
        }

        PlayerEntity player = (PlayerEntity)(Object) this;

        // --- 生命恢复 1.5x ---
        if (player.getHealth() < player.getMaxHealth()) {
            this.timelord$regenBoostTimer++;
            if (this.timelord$regenBoostTimer >= 160) {
                this.timelord$regenBoostTimer = 0;
                if (player.getHungerManager().getFoodLevel() > 0) {
                    player.heal(1.0f);
                    player.getHungerManager().addExhaustion(1.0f);
                }
            }
        } else {
            this.timelord$regenBoostTimer = 0;
        }

        // --- 氧气消耗 75% ---
        if (player.isSubmergedInWater() && TimelordAbilities.shouldCompensateAirLoss(player.age)) {
            int maxAir = player.getMaxAir();
            int currentAir = player.getAir();
            if (currentAir < maxAir) {
                player.setAir(Math.min(maxAir, currentAir + 1));
            }
        }
    }
}