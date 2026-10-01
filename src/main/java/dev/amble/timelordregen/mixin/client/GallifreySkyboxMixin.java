package dev.amble.timelordregen.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.dimensions.RegenerationDimensions;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WorldRenderer.class, priority = 1001)
public class GallifreySkyboxMixin {

    @Shadow
    private @Nullable ClientWorld world;

    @Unique
    private static final Identifier SUN = RegenerationMod.id("textures/environment/gallifreyan_suns.png");

    @ModifyExpressionValue(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;SUN:Lnet/minecraft/util/Identifier;"))
    private Identifier timelordRegen$sun(Identifier sun) {
        return this.world.getRegistryKey() == RegenerationDimensions.GALLIFREY ? SUN : sun;
    }

}
