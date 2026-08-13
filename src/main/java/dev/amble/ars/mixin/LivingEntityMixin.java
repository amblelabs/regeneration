package dev.amble.ars.mixin;

import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method="damage", at=@At("HEAD"), cancellable=true)
    private void regeneration$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
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