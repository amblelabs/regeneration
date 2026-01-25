package dev.amble.timelordregen.mixin.boat;

import dev.amble.timelordregen.api.boat.BoatTypeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChestBoatEntity.class, priority = 999)
public abstract class ChestBoatEntityMixin extends BoatEntity {

    public ChestBoatEntityMixin(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "asItem", at = @At("HEAD"), cancellable = true)
    public void asItem(CallbackInfoReturnable<Item> cir) {
        Item item = BoatTypeRegistry.getItem(this.getVariant(), true);

        if (item != null)
            cir.setReturnValue(item);
    }
}
