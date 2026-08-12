package ars.data;

import ars.RegenerationMod;
import ars.core.RegenerationCore;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import com.mojang.serialization.Codec;

public class Attachments {
    public static final AttachmentType<RegenerationCore> REGENERATION = AttachmentRegistry.<RegenerationCore>builder()
            .persistent(RegenerationCore.CODEC)
            .initializer(RegenerationCore::new)
            .copyOnDeath()
            .buildAndRegister(RegenerationMod.id("regeneration"));

    /**
     * 时间领主标记 Data Attachment
     * 用于判断玩家是否拥有重生能力
     */
    public static final AttachmentType<Boolean> IS_TIMELORD = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .initializer(() -> Boolean.FALSE)
            .copyOnDeath()
            .buildAndRegister(RegenerationMod.id("is_timelord"));

    public static void init() {
        // 初始化时自动注册
    }
}