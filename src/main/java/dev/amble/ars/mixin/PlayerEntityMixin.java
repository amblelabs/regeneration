package dev.amble.ars.mixin;

import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.data.Attachments;
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

    /**
     * 检查玩家是否为时间领主。
     * 使用 Data Attachment 存储，自动序列化，无需 Mixin 注入 NBT。
     * FIX: 客户端如果 IS_TIMELORD 未同步，但 REGENERATION 已同步，也视为时间领主
     */
    @Override
    public boolean isTimelord() {
        PlayerEntity player = self();

        // 先检查 IS_TIMELORD attachment
        Boolean value = player.getAttached(Attachments.IS_TIMELORD);
        if (value != null && value) return true;

        // 客户端回退：如果 RegenerationInfo 已同步到客户端，推断为时间领主
        // 这解决了同步包到达前或 IS_TIMELORD 未同步时的判断问题
        if (player.getWorld().isClient) {
            return player.getAttached(Attachments.REGENERATION) != null;
        }

        return false;
    }

    /**
     * 设置玩家的时间领主状态。
     * 使用 Data Attachment，自动保存到玩家 NBT。
     */
    @Override
    public void setTimelord(boolean timelord) {
        PlayerEntity player = self();
        player.setAttached(Attachments.IS_TIMELORD, timelord);

        // 如果设为时间领主，确保重生信息已初始化
        if (timelord) {
            RegenerationCore info = player.getAttachedOrCreate(Attachments.REGENERATION, RegenerationCore::new);
            if (info.getUsesLeft() <= 0) {
                info.setUsesLeft(0);
            }
        }
    }

    /**
     * 覆盖默认实现：只有时间领主才返回 RegenerationInfo
     */
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