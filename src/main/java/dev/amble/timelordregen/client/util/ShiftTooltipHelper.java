package dev.amble.timelordregen.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ShiftTooltipHelper {
    /**
     * 检测玩家是否按住 Shift 键。
     * - 若按住：调用 TooltipHelper 将 longText 按 '*' 拆分为多行添加到 tooltip。
     * - 否则：添加一条灰色的提示文本 "按住 Shift 查看更多"。
     */
    public static void addShiftTooltip(List<Text> tooltip, Text longText) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shiftPressed = false;
        if (client.getWindow() != null) {
            long handle = client.getWindow().getHandle();
            shiftPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        }
        if (shiftPressed) {
            TooltipHelper.addWrappedTooltip(tooltip, longText);
        } else {
            tooltip.add(Text.translatable("tooltip.ars.hold_shift")
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }
}