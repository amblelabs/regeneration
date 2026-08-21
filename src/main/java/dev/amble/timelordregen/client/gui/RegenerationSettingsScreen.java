package dev.amble.timelordregen.client.gui;

import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.core.RegenerationCore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RegenerationSettingsScreen extends Screen {

    private final PlayerEntity player;
    private final RegenerationCore info;

    private static final int COLOR_OVERLAY      = 0xE6000000;
    private static final int COLOR_PANEL        = 0xFF1A0A0E;
    private static final int COLOR_PANEL_BORDER = 0xFF8B6914;
    private static final int COLOR_GOLD         = 0xFFC9A227;
    private static final int COLOR_GOLD_DIM     = 0xFF8B6914;
    private static final int COLOR_GLOW         = 0xFFFFD700;
    private static final int COLOR_TEXT         = 0xFFFFF0E0;
    private static final int COLOR_TEXT_DIM     = 0xFFAA9988;
    private static final int COLOR_CARD_BG      = 0xFF0D0408;
    private static final int COLOR_CARD_BORDER  = 0xFF5C3A1E;
    private static final int COLOR_BTN_BG       = 0xFF2D0A12;
    private static final int COLOR_BTN_HOVER    = 0xFF4A1020;
    private static final int COLOR_RUNE         = 0xFF6B4226;

    private static final int PANEL_WIDTH  = 280;
    private static final int PANEL_HEIGHT = 250;

    private int panelX, panelY;

    public RegenerationSettingsScreen(PlayerEntity player) {
        super(Text.translatable("gui.regen.settings.title"));
        this.player = player;
        this.info = RegenerationCore.get(player);
    }

    @Override
    protected void init() {

        if (this.info == null || !((RegenerationCapable) player).isTimelord()) {
            this.close();
            return;
        }

        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        int cx = panelX + PANEL_WIDTH / 2;
        int cy = panelY + 118;

        boolean changeSkin = info.isChangeSkinOnRegen();
        this.addDrawableChild(new TimeLordButton(
                cx - 100, cy, 200, 20,
                Text.translatable("gui.regen.settings.toggle_skin_change",
                        Text.translatable(changeSkin ? "gui.regen.settings.on" : "gui.regen.settings.off")),
                button -> {
                    boolean newValue = !info.isChangeSkinOnRegen();
                    info.setChangeSkinOnRegen(newValue);

                    var buf = PacketByteBufs.create();
                    buf.writeBoolean(newValue);
                    ClientPlayNetworking.send(RegenerationCore.UPDATE_SKIN_PACKET, buf);

                    button.setMessage(Text.translatable("gui.regen.settings.toggle_skin_change",
                            Text.translatable(newValue ? "gui.regen.settings.on" : "gui.regen.settings.off")));
                }
        ));

        this.addDrawableChild(new TimeLordButton(
                cx - 100, cy + 28, 200, 20,
                Text.translatable("gui.regen.settings.reset_skin"),
                button -> ClientPlayNetworking.send(RegenerationCore.RESET_SKIN_PACKET, PacketByteBufs.empty())
        ));

        if (FabricLoader.getInstance().isModLoaded("ait")) {
            int tardisMode = info.getTardisInteriorMode();
            this.addDrawableChild(new TimeLordButton(
                    cx - 100, cy + 56, 200, 20,
                    getTardisModeText(tardisMode),
                    button -> {
                        int newMode = (info.getTardisInteriorMode() + 1) % 3;
                        info.setTardisInteriorMode(newMode);

                        var buf = PacketByteBufs.create();
                        buf.writeInt(newMode);
                        ClientPlayNetworking.send(RegenerationCore.UPDATE_TARDIS_MODE_PACKET, buf);

                        button.setMessage(getTardisModeText(newMode));
                    }
            ));
        }

        this.addDrawableChild(new TimeLordButton(
                cx - 50, cy + 90, 100, 20,
                Text.translatable("gui.regen.settings.done"),
                button -> this.close()
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, COLOR_OVERLAY);

        ctx.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF080808);
        ctx.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        drawThickBorder(ctx, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, COLOR_PANEL_BORDER, 2);
        ctx.drawBorder(panelX + 2, panelY + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, COLOR_GOLD_DIM);

        String playerName = player.getName().getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(playerName).formatted(Formatting.GOLD),
                panelX + PANEL_WIDTH / 2, panelY + 6, COLOR_GOLD_DIM);

        String title = Text.translatable("gui.regen.settings.title").getString();
        int titleWidth = this.textRenderer.getWidth(title);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(title).formatted(Formatting.WHITE),
                panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 18, COLOR_TEXT);

        drawRuneLine(ctx, panelX + 20, panelY + 32, PANEL_WIDTH - 40);

        int cardX = panelX + 16;
        int cardY = panelY + 42;
        int cardW = PANEL_WIDTH - 32;
        int cardH = 40;

        ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_CARD_BG);
        ctx.drawBorder(cardX, cardY, cardW, cardH, COLOR_CARD_BORDER);

        int remaining = info.getUsesLeft();
        String remainingText = Text.translatable("gui.regen.settings.remaining", remaining).getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(remainingText).formatted(Formatting.GOLD),
                panelX + PANEL_WIDTH / 2, cardY + cardH / 2 - 4, COLOR_GOLD_DIM);

        int statusY = cardY + cardH + 10;
        Text statusText = getStatusText();
        int statusColor = getStatusColor();
        int statusWidth = this.textRenderer.getWidth(statusText);
        ctx.drawTextWithShadow(this.textRenderer, statusText,
                panelX + (PANEL_WIDTH - statusWidth) / 2, statusY, statusColor);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private Text getStatusText() {
        if (info.isRegenerating()) {
            return Text.translatable("gui.regen.settings.status.regenerating").formatted(Formatting.GOLD);
        } else if (info.getDelay().isRunning()) {
            return Text.translatable("gui.regen.settings.status.delay");
        } else if (info.isConfused()) {
            return Text.translatable("gui.regen.settings.status.confused");
        } else if (info.isInvulnerable()) {
            return Text.translatable("gui.regen.settings.status.invulnerable");
        } else if (info.getUsesLeft() <= 0) {
            return Text.translatable("gui.regen.settings.status.exhausted").formatted(Formatting.RED);
        }
        return Text.translatable("gui.regen.settings.status.normal");
    }

    private int getStatusColor() {
        if (info.isRegenerating()) return COLOR_GLOW;
        if (info.getDelay().isRunning()) return COLOR_GOLD;
        if (info.isConfused()) return 0xFFFF55FF;
        if (info.isInvulnerable()) return 0xFF55FF55;
        if (info.getUsesLeft() <= 0) return COLOR_GOLD_DIM;
        return COLOR_TEXT_DIM;
    }

    private void drawRuneLine(DrawContext ctx, int x, int y, int w) {
        int center = x + w / 2;
        ctx.fill(x, y, x + w / 2 - 8, y + 1, COLOR_RUNE);
        ctx.fill(x + w / 2 + 8, y, x + w, y + 1, COLOR_RUNE);
        ctx.fill(center - 2, y - 2, center + 2, y + 3, COLOR_GOLD_DIM);
        ctx.fill(center - 1, y - 1, center + 1, y + 2, COLOR_GLOW);
        ctx.fill(x + 4, y - 1, x + 6, y + 2, COLOR_GOLD_DIM);
        ctx.fill(x + w - 6, y - 1, x + w - 4, y + 2, COLOR_GOLD_DIM);
    }

    private void drawThickBorder(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        for (int i = 0; i < thickness; i++) {
            ctx.drawBorder(x + i, y + i, w - i * 2, h - i * 2, color);
        }
    }

    private static Text getTardisModeText(int mode) {
        String subKey = switch (mode) {
            case 1 -> "disabled";
            case 2 -> "refurbish";
            default -> "enabled";
        };
        return Text.translatable("gui.regen.settings.tardis_mode",
                Text.translatable("gui.regen.settings.tardis_mode." + subKey));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class TimeLordButton extends ButtonWidget {
        TimeLordButton(int x, int y, int w, int h, Text msg, PressAction action) {
            super(x, y, w, h, msg, action, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = getX(), ty = getY(), tw = getWidth(), th = getHeight();
            boolean hovered = isHovered();

            int bg = hovered ? COLOR_BTN_HOVER : COLOR_BTN_BG;
            ctx.fill(tx, ty, tx + tw, ty + th, bg);
            ctx.drawBorder(tx, ty, tw, th, hovered ? COLOR_GOLD : COLOR_CARD_BORDER);

            if (hovered) {
                ctx.fill(tx + 1, ty + 1, tx + tw - 1, ty + 2, COLOR_GLOW);
            }

            Text msg = getMessage();
            int mw = textRenderer.getWidth(msg);
            int color = this.active ? (hovered ? COLOR_GLOW : COLOR_TEXT) : 0xFF555555;
            ctx.drawTextWithShadow(textRenderer, msg, tx + (tw - mw) / 2, ty + (th - 8) / 2, color);
        }
    }
}