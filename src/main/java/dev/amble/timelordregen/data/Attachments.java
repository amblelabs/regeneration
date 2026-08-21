package dev.amble.timelordregen.data;

import com.mojang.serialization.Codec;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.core.RegenerationCore;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public class Attachments {
    public static final AttachmentType<RegenerationCore> REGENERATION = AttachmentRegistry.<RegenerationCore>builder()
            .persistent(RegenerationCore.CODEC)
            .initializer(RegenerationCore::new)
            .copyOnDeath()
            .buildAndRegister(RegenerationMod.id("regeneration"));

    /**
     * 时间领主标记
     */
    public static final AttachmentType<Boolean> IS_TIMELORD = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .initializer(() -> Boolean.FALSE)
            .copyOnDeath()
            .buildAndRegister(RegenerationMod.id("is_timelord"));

    public static void init() {
    }
}