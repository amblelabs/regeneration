package dev.amble.timelordregen.mixin;

import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.core.energy.RegenEnergy;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void regeneration$remove(ServerPlayerEntity player, CallbackInfo ci) {
        RegenerationCore.onDisconnect(player);
        RegenEnergy.onDisconnect(player);
    }
}
