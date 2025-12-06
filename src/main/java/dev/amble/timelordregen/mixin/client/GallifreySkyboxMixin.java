package dev.amble.timelordregen.mixin.client;

import dev.amble.timelordregen.RegenerationMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import util.GallifreyanSkybox;

@Mixin(value = WorldRenderer.class, priority = 1001)
public class GallifreySkyboxMixin {

    @Shadow
    private @Nullable ClientWorld world;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private VertexBuffer starsBuffer;

    @Shadow
    private VertexBuffer lightSkyBuffer;

    @Shadow
    private VertexBuffer darkSkyBuffer;

    @Shadow
    @Final
    private static Identifier MOON_PHASES;

    @Unique
    private static final Identifier SUN = RegenerationMod.id("textures/environment/gallifreyan_suns.png");

    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    public void timelordRegen$renderSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera,
                              boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (this.world == null)
            return;

        if (this.world.getRegistryKey() == ClientWorld.OVERWORLD/*RegenerationModDimensions.GALLIFREY*/) {
            GallifreyanSkybox.renderSky(client, starsBuffer, lightSkyBuffer, darkSkyBuffer, world, SUN, MOON_PHASES, matrices, projectionMatrix, tickDelta, camera, thickFog, fogCallback);
            ci.cancel();
        }
    }

}
