package dev.amble.ars.api;

import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.data.Attachments;
import net.minecraft.entity.LivingEntity;

import java.util.Optional;

public interface RegenerationCapable {
    default RegenerationCore getRegenerationInfo() {
        if (!(this instanceof LivingEntity living)) throw new UnsupportedOperationException("This method is only default for LivingEntity instances. Override it and implement it");
        return getLivingInfo(living);
    }

    default Optional<RegenerationCore> withInfo() {
        return Optional.ofNullable(this.getRegenerationInfo());
    }

    default void tickRegeneration() {
        if (!this.isTimelord()) return;
        RegenerationCore info = this.getRegenerationInfo();
        if (info != null) {
            if (!(this instanceof LivingEntity living)) throw new UnsupportedOperationException("This method is only default for LivingEntity instances. Override it and implement it");
            info.tick(living);
        }
    }

    default boolean isTimelord() {
        return true;
    }

    default void setTimelord(boolean timelord) {
        // 默认空实现
    }

    static RegenerationCore getLivingInfo(LivingEntity entity) {
        return entity.getAttached(Attachments.REGENERATION);
    }
}