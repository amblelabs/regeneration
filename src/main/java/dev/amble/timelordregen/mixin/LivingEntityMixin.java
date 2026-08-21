package dev.amble.timelordregen.mixin;

import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.core.RegenerationCore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * 重生后伤害减免
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float regeneration$modifyDamage(float amount, DamageSource source) {
        if (this instanceof RegenerationCapable capable) {
            RegenerationCore info = capable.getRegenerationInfo();
            if (info != null) {
                return info.applyDamageReduction((LivingEntity)(Object)this, source, amount);
            }
        }
        return amount;
    }

    /**
     * 重生过程中免疫伤害
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void regeneration$cancelDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount == Float.MAX_VALUE) return;

        if (this instanceof RegenerationCapable capable) {
            RegenerationCore info = capable.getRegenerationInfo();
            if (info == null) return;

            if (info.isActive()) {
                cir.setReturnValue(false);
            }
        }
    }
}