package dev.amble.timelordregen.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amble.timelordregen.dimensions.RegenerationDimensions;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @WrapOperation(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setTimeOfDay(J)V"))
    private void regeneration$skipNight(ServerWorld world, long timeOfDay, Operation<Void> original) {
        if (world.getRegistryKey() == RegenerationDimensions.GALLIFREY)
            world = world.getServer().getOverworld();

        original.call(world, timeOfDay);
    }
}
