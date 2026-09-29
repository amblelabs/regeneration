package dev.amble.timelordregen.client.energy;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.client.config.RegenerationClientConfig;
import dev.amble.timelordregen.core.RegenerationCore;
import dev.amble.timelordregen.core.energy.EnergyAbility;
import dev.amble.timelordregen.core.energy.EnergyFxType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class EnergyHud {
    private static final Identifier VIGNETTE = RegenerationMod.id("textures/gui/delay_overlay.png");

    private static final int BG = 0xFF0D0408, BORDER = 0xFF8B6914, GOLD = 0xFFC9A227, GLOW = 0xFFFFD700;
    private static final int WIDTH = 91, HEIGHT = 4;

    private static float fade, fadeAt;

    static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.options.hudHidden) return;
        if (!(p instanceof RegenerationCapable cap) || !cap.isTimelord()) return;
        RegenerationCore info = RegenerationCore.get(p);
        if (info == null) return;

        EnergyClient.ChannelView local = EnergyClient.local();
        float time = p.age + tickDelta;

        float now = mc.world.getTime() + tickDelta;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            if (local != null && local.ability == EnergyAbility.BLAST && RegenerationClientConfig.get().vignette) {
                fade = vignette(local, now, time, tickDelta);
                fadeAt = now;
                overlay(ctx, fade);
            } else if (now >= fadeAt && now - fadeAt < 8.0f) {
                overlay(ctx, fade * (1.0f - (now - fadeAt) / 8.0f));
            }
            float g = EnergyFx.glare(tickDelta);
            if (g > 0.0f) ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), ((int) (g * 255.0f) << 24) | 0xFFF5C0);
        }

        RegenerationClientConfig.HudMode hud = RegenerationClientConfig.get().hud;
        if (hud == RegenerationClientConfig.HudMode.HIDDEN) return;
        if (hud == RegenerationClientConfig.HudMode.ACTIVE && local == null && !EnergyClient.recentlyChanged() && !EnergyClient.held() && !EnergyClient.home) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int y;
        if (mc.interactionManager != null && mc.interactionManager.hasStatusBars()) {
            float hp = Math.max(p.getMaxHealth(), p.getHealth());
            int rows = MathHelper.ceil((hp + MathHelper.ceil(p.getAbsorptionAmount())) / 2.0f / 10.0f);
            int rh = Math.max(10 - (rows - 2), 3);
            int top = sh - 39 - (rows - 1) * rh - (p.getArmor() > 0 ? 10 : 0);
            y = Math.min(top, sh - 84) - 8;
        } else {
            y = sh - 31;
        }
        int x = sw / 2 - WIDTH / 2;
        int e = EnergyClient.energy;

        if (local != null) {
            float pulse = 0.5f + 0.5f * MathHelper.sin(time * 0.3f);
            ctx.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 2, ((int) (40 + pulse * 110) << 24) | (GLOW & 0xFFFFFF));
        }
        ctx.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, BORDER);
        ctx.fill(x, y, x + WIDTH, y + HEIGHT, BG);

        int per = EnergyClient.perRegen;
        int fill = WIDTH * Math.min(e, per) / per;
        if (fill > 0) {
            ctx.fill(x, y, x + fill, y + HEIGHT, GOLD);
            ctx.fill(x, y, x + fill, y + 1, GLOW);
        }
        int over = WIDTH * MathHelper.clamp(e - per, 0, per) / per;
        if (over > 0) ctx.fill(x, y + HEIGHT - 2, x + over, y + HEIGHT, 0xFFFFF5C0);

        Text label = Text.translatable("hud.timelordregen.energy", e, info.getUsesLeft());
        ctx.drawTextWithShadow(mc.textRenderer, label, x + WIDTH + 4, y - 2, GOLD);
    }

    private static float vignette(EnergyClient.ChannelView v, float now, float time, float tickDelta) {
        if (v.phase == EnergyFxType.CHARGE)
            return 0.35f * MathHelper.clamp((now - v.phaseTick) / EnergyClient.chargeTicks, 0.0f, 1.0f);
        return 0.22f + 0.08f * MathHelper.sin(time * 0.25f) + 0.45f * EnergyFx.kick(tickDelta);
    }

    private static void overlay(DrawContext ctx, float opacity) {
        if (opacity < 0.01f) return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, opacity));
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        ctx.drawTexture(VIGNETTE, 0, 0, 0, 0, w, h, w, h);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
