package dev.amble.timelordregen.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.client.sound.PlayerFollowingLoopingSound;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.data.tree.RegenerationSounds;
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

        float time = mc.player.age + tickDelta;
        float period = 5 * 20;
        float pulse = (float) (0.5 * (0.5 + 0.5 * Math.sin(2 * Math.PI * time / period)));
        float opacity = Math.max(0, Math.min(0.5f, pulse));

        if (SOUND == null || !mc.getSoundManager().isPlaying(SOUND)) {
            SOUND = new PlayerFollowingLoopingSound(RegenerationSounds.SWING_REGEN_LOOP, SoundCategory.PLAYERS, opacity * 0.5f);
            mc.getSoundManager().play(SOUND);
        } else {
            SOUND.setVolume(opacity * 0.5f);
        }

        if (opacity < 0.01f) return;

        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        context.drawTexture(TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}