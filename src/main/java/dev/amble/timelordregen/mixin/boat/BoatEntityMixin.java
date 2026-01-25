package dev.amble.timelordregen.mixin.boat;

import dev.amble.timelordregen.api.boat.BoatTypeRegistry;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BoatEntity.class, priority = 999)
public abstract class BoatEntityMixin {

    @Shadow public abstract BoatEntity.Type getVariant();

    @Inject(method = "asItem", at = @At("HEAD"), cancellable = true)
    public void asItem(CallbackInfoReturnable<Item> cir) {
        Item item = BoatTypeRegistry.getItem(this.getVariant(), false);

        if (item != null)
            cir.setReturnValue(item);
    }
}
