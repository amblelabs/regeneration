package dev.amble.ars.mixin.compat.ait;

import dev.amble.ait.client.renderers.entities.ControlEntityRenderer;
import dev.amble.ait.core.entities.ConsoleControlEntity;
import dev.amble.ars.api.RegenerationCapable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ControlEntityRenderer.class)
public class ControlEntityRendererMixin {

    @Unique
    private static boolean regen$isClientTimelord() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        if (!(player instanceof RegenerationCapable capable)) return false;
        return capable.isTimelord();
    }

    @Inject(method = "isScanningSonicInConsole", at = @At("HEAD"), cancellable = true, remap = false)
    private static void regen$showFlightEvents(ConsoleControlEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (regen$isClientTimelord()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPlayerLookingAtControlWithSonic", at = @At("HEAD"), cancellable = true, remap = false)
    private static void regen$showControlNames(HitResult hitResult, ConsoleControlEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!regen$isClientTimelord()) return;

        if (!(hitResult instanceof EntityHitResult entityHit)) return;
        Entity hitEntity = entityHit.getEntity();
        if (hitEntity == null || !hitEntity.equals(entity)) return;

        cir.setReturnValue(true);
    }
}