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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ControlEntityRenderer.class)
public class ControlEntityRendererMixin {

    @Inject(method = "isScanningSonicInConsole", at = @At("HEAD"), cancellable = true, remap = false)
    private static void regen$showFlightEvents(ConsoleControlEntity entity, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        // 不是时间领主？那您就是普通人类，老老实实拿音速起子去
        if (!(player instanceof RegenerationCapable capable) || !capable.isTimelord()) {
            return;
        }

        // 是时间领主就有高维感知，不需要管还剩几次重生
        cir.setReturnValue(true);
    }

    @Inject(method = "isPlayerLookingAtControlWithSonic", at = @At("HEAD"), cancellable = true, remap = false)
    private static void regen$showControlNames(HitResult hitResult, ConsoleControlEntity entity, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        // 同理：先判定种族，不是时间领主直接放行（走原逻辑）
        if (!(player instanceof RegenerationCapable capable) || !capable.isTimelord()) {
            return;
        }

        // 确认玩家确实在看着这个按钮
        if (!(hitResult instanceof EntityHitResult entityHit)) {
            return;
        }

        Entity hitEntity = entityHit.getEntity();
        if (hitEntity == null || !hitEntity.equals(entity)) {
            return;
        }

        // 时间领主的双眼自带解析能力，不需要音速起子
        cir.setReturnValue(true);
    }
}