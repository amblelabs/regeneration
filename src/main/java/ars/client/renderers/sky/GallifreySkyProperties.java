package ars.client.renderers.sky;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Gallifrey 维度的自定义天空效果。
 * 继承自 DimensionEffects，完全自主控制天空、雾和云的颜色。
 */
public class GallifreySkyProperties extends DimensionEffects {

    // 黄昏/日落时的雾颜色数组（R, G, B, 混合因子）
    public static final float[] SUNSET_COLORS = {0.6f, 0.05f, 0.05f, 1.0f};

    /**
     * 默认构造方法，使用主世界的云层高度（192）并启用地面。
     */
    public GallifreySkyProperties() {
        super(Overworld.CLOUDS_HEIGHT, true, SkyType.NORMAL, false, false);
    }

    /**
     * 自定义参数的构造方法（如果您需要微调云层高度等）。
     */
    public GallifreySkyProperties(float cloudLevel, boolean hasGround, SkyType skyType,
                                  boolean forceBrightLightmap, boolean constantAmbientLight) {
        super(cloudLevel, hasGround, skyType, forceBrightLightmap, constantAmbientLight);
    }

    /**
     * 根据太阳高度角调整雾的颜色，实现日出/日落的渐变效果。
     * 这里将颜色乘上一个基于太阳高度的衰减系数，使雾色随太阳位置变化。
     */
    @Override
    public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
        // 分别对 R, G, B 通道乘以不同的衰减因子，制造暖色调偏移
        return color.multiply(sunHeight * 0.91f + 0.09f,
                sunHeight * 0.94f + 0.06f,
                sunHeight * 0.94f + 0.06f);
    }

    /**
     * 是否在特定坐标启用浓雾（例如在低处使用厚重雾效）。
     * 这里返回 false，表示不额外增加厚度。
     */
    @Override
    public boolean useThickFog(int camX, int camY) {
        return false;
    }

    /**
     * 覆写特定时间的雾颜色（用于黄昏/日出时的特殊着色）。
     * 如果太阳高度在 -0.5 ~ 0.5 之间（即接近地平线），则使用 SUNSET_COLORS 数组
     * 动态计算黄昏色调；否则返回 null（使用普通雾色）。
     */
    @Override
    public float @Nullable [] getFogColorOverride(float timeOfDay, float tickDelta) {
        // 计算当前时间的余弦值，判断是否接近日落/日出
        float cosVal = MathHelper.cos(timeOfDay * ((float) Math.PI * 2)) - 0.0f;
        if (cosVal >= -0.5f && cosVal <= 0.5f) {
            // 插值参数 i 范围 0~1，表示黄昏的程度
            float i = (cosVal - -0.0f) / 0.7f * 0.5f + 0.5f;
            float j = 1.0f - (1.0f - MathHelper.sin(i * (float) Math.PI)) * 0.99f;
            j *= j;
            // 动态调整 SUNSET_COLORS 数组中的 RGB 值，使其随黄昏程度变化
            SUNSET_COLORS[2] = i * 0.1f + 0.7f;   // 蓝色通道
            SUNSET_COLORS[1] = i * i * 0.7f + 0.2f; // 绿色通道
            SUNSET_COLORS[0] = i * i * 0.3f + 0.2f; // 红色通道
            SUNSET_COLORS[3] = j;                  // 混合因子
            return SUNSET_COLORS;
        }
        return null;
    }
}