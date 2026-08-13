package dev.amble.ars.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 服务器端粒子工具类。
 * 重生粒子已完全移至客户端生成，此类保留仅作兼容或未来服务器端特效使用。
 */
public class ParticleUtil {
    private final boolean hasHead;
    public ParticleUtil(boolean hasHead) {
        this.hasHead = hasHead;
    }

    public void spawnParticles(LivingEntity entity, ServerWorld serverWorld) {
        // 客户端已接管所有重生粒子生成，服务器端不再发送粒子包
        // 如需服务器端权威判断（如切石机检测），可在此发送自定义网络包通知客户端
    }
}