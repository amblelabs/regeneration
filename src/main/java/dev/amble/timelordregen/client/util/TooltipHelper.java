package dev.amble.timelordregen.client.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;

public class TooltipHelper {
    /**
     * 将原始文本按 '*' 拆分成多行，并尽可能保留原样式（颜色）。
     * 如果原始文本包含 § 颜色代码，也会被识别。
     */
    public static void addWrappedTooltip(List<Text> tooltip, Text originalText) {
        String raw = originalText.getString();
        // 1. 尝试从 Style 获取颜色
        TextColor color = originalText.getStyle().getColor();
        // 2. 如果 style 没有颜色，尝试从字符串中提取 § 代码
        String colorCode = "";
        if (color == null) {
            int index = raw.indexOf('§');
            if (index != -1 && index + 1 < raw.length()) {
                char code = raw.charAt(index + 1);
                colorCode = "§" + code;
            }
        }
        String[] lines = raw.split("\\*");
        for (String line : lines) {
            if (!line.isEmpty()) {
                MutableText text = Text.literal(line);
                if (color != null) {
                    text = text.setStyle(Style.EMPTY.withColor(color));
                } else if (!colorCode.isEmpty()) {
                    // 直接拼接 § 代码（不推荐，但兼容旧用法）
                    text = Text.literal(colorCode + line);
                }
                tooltip.add(text);
            }
        }
    }
}