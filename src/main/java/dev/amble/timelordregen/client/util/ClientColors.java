package dev.amble.timelordregen.client.util;

import dev.amble.timelordregen.core.RegenerationModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.world.BiomeColors;

@Environment(EnvType.CLIENT)
public class ClientColors {

    private static final int RED = 0x4B2F26;

    public static void registerGallifreyGrassColor() {
        ColorProviderRegistry.BLOCK.register(
                (state, world, pos, tintIndex) -> {
                    if (world != null && pos != null) {
                        int biomeColor = BiomeColors.getGrassColor(world, pos);
                        float factor = getColorBrightnessFactor(biomeColor) * 0.1f;
                        return blendColors(RED, biomeColor, factor);
                    } else {
                        return RED;
                    }
                }
               // RegenerationModBlocks.GALLIFREY_GRASS_BLOCK
        );
    }

    private static int blendColors(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 * (1 - factor) + r2 * factor);
        int g = (int) (g1 * (1 - factor) + g2 * factor);
        int b = (int) (b1 * (1 - factor) + b2 * factor);

        return (r << 16) | (g << 8) | b;
    }

    private static float getColorBrightnessFactor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }
}
