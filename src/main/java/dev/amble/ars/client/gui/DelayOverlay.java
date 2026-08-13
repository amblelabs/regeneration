package dev.amble.ars.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.amble.ars.RegenerationMod;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.client.sound.PlayerFollowingLoopingSound;
import dev.amble.ars.data.tree.RegenerationSounds;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class DelayOverlay implements HudRenderCallback {
    private static final Identifier TEXTURE = RegenerationMod.id("textures/gui/delay_overlay.png");
    private static PlayerFollowingLoopingSound SOUND;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // ★ 关键新增：玩家死亡/重生期间强制停止渲染和音效
        if (!mc.player.isAlive()) {
            if (SOUND != null) {
                mc.getSoundManager().stop(SOUND);
                SOUND = null;
            }
            return;
        }

        RegenerationCore info = RegenerationCore.get(mc.player);
        boolean active = info != null && !info.isRegenerating() && info.getDelay().isRunning();

        if (!active) {
            if (SOUND != null) {
                mc.getSoundManager().stop(SOUND);
                SOUND = null;
            }
            return;
        }

        // 计算脉冲透明度：周期 5 秒（100 ticks），范围 0 ~ 0.5
        float time = mc.player.age + tickDelta;
        float period = 5 * 20; // 100 ticks
        float pulse = (float) (0.5 * (0.5 + 0.5 * Math.sin(2 * Math.PI * time / period)));
        float opacity = Math.max(0, Math.min(0.5f, pulse)); // 严格限制在 0~0.5

        // 管理循环音效，音量跟随透明度（最大音量 25%）
        if (SOUND == null || !mc.getSoundManager().isPlaying(SOUND)) {
            SOUND = new PlayerFollowingLoopingSound(RegenerationSounds.SWING_REGEN_LOOP, SoundCategory.PLAYERS, opacity * 0.5f);
            mc.getSoundManager().play(SOUND);
        } else {
            SOUND.setVolume(opacity * 0.5f);
        }

        // 如果透明度太低，跳过渲染（但音效继续）
        if (opacity < 0.01f) return;

        // 仅在第一人称视角下显示叠加层
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

        // 渲染叠加层
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 设置颜色（白色，透明度为 opacity）
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        context.drawTexture(TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

        // 恢复颜色（避免影响其他渲染）
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}