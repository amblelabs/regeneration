package dev.amble.timelordregen.mixin;

import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.data.Attachments;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements RegenerationCapable {

    @Unique
    private PlayerEntity self() {
        return (PlayerEntity)(Object) this;
    }

    @Override
    public boolean isTimelord() {
        PlayerEntity player = self();
        Boolean value = player.getAttached(Attachments.IS_TIMELORD);
        if (value != null && value) return true;

        if (player.getWorld() != null && player.getWorld().isClient) {
            return player.getAttached(Attachments.REGENERATION) != null;
        }

        return false;
    }

    @Override
    public void setTimelord(boolean timelord) {
        PlayerEntity player = self();
        player.setAttached(Attachments.IS_TIMELORD, timelord);

        if (timelord) {
            player.getAttachedOrCreate(Attachments.REGENERATION, RegenerationCore::new);
        } else {
            player.removeAttached(Attachments.REGENERATION);
        }
    }

    @Override
    public RegenerationCore getRegenerationInfo() {
        if (!this.isTimelord()) return null;
        return RegenerationCapable.super.getRegenerationInfo();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void regeneration$tick(CallbackInfo ci) {
        this.tickRegeneration();
    }
}