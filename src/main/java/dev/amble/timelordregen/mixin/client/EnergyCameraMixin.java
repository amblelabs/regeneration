package dev.amble.timelordregen.mixin.client;

import dev.amble.timelordregen.client.energy.EnergyFx;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class EnergyCameraMixin {
    @Shadow
    private int ticks;

    @Inject(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"))
    private void regen$energyShake(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        float s = EnergyFx.shake(tickDelta);
        if (s <= 0.0f) return;
        float t = this.ticks + tickDelta;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s * (0.8f * MathHelper.sin(t * 0.9f) + 0.3f * MathHelper.sin(t * 2.1f + 1.1f))));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * (0.8f * MathHelper.sin(t * 0.9f + 2.0f) + 0.3f * MathHelper.sin(t * 2.1f + 0.4f))));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s * 0.5f * MathHelper.sin(t * 1.3f)));
    }
}
