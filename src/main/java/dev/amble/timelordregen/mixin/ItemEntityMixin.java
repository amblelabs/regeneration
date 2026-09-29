package dev.amble.timelordregen.mixin;

import dev.amble.timelordregen.core.RegenerationModItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity entity = (ItemEntity) (Object) this;
        ItemStack stack = entity.getStack();

        if (stack.isEmpty()) return;

        if (isAzbantiumOrWhitePointStar(stack.getItem())) {
            if (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION)) {
                cir.setReturnValue(false);
            }
        }
    }

    private static boolean isAzbantiumOrWhitePointStar(Item item) {
        return item == RegenerationModItems.AZBANTIUM_INGOT ||
                item == RegenerationModItems.AZBANTIUM_AXE ||
                item == RegenerationModItems.AZBANTIUM_BATTLEAXE ||
                item == RegenerationModItems.AZBANTIUM_HOE ||
                item == RegenerationModItems.AZBANTIUM_PICKAXE ||
                item == RegenerationModItems.AZBANTIUM_SWORD ||
                item == RegenerationModItems.WHITE_POINT_STAR;
    }
}