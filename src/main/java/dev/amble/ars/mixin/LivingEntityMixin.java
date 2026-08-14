package dev.amble.ars.mixin;

import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
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
     * 细胞能量富裕期：动态减免伤害数值
     * 在 damage() 方法入口处修改 amount 参数
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
     * 重生过程中（delay/queued/regenerating）：完全免疫伤害
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