package ars.mixin.origin;

import ars.compat.origin.OriginCompat;
import io.github.apace100.origins.component.PlayerOriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerOriginComponent.class, remap = false)
public class PlayerOriginComponentMixin {
    @Shadow
    private PlayerEntity player;

    @Inject(method = "setOrigin", at = @At("TAIL"), remap = false)
    public void regeneration$setOrigin(OriginLayer layer, Origin origin, CallbackInfo ci) {
        OriginCompat.setupRegenerationPower(this.player);
    }
}