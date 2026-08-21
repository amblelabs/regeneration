package dev.amble.timelordregen.client.renderers.sky;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Gallifrey
 */
@Environment(EnvType.CLIENT)
public class GallifreySkyProperties extends DimensionEffects {

    public GallifreySkyProperties() {
        super(Overworld.CLOUDS_HEIGHT, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
        return color.multiply(
                sunHeight * 0.91f + 0.09f,
                sunHeight * 0.94f + 0.06f,
                sunHeight * 0.94f + 0.06f
        );
    }

    @Override
    public boolean useThickFog(int camX, int camY) {
        return false;
    }

    @Override
    public float @Nullable [] getFogColorOverride(float timeOfDay, float tickDelta) {
        float cosVal = MathHelper.cos(timeOfDay * ((float) Math.PI * 2));
        if (cosVal >= -0.5f && cosVal <= 0.5f) {
            float i = (cosVal / 0.7f) * 0.5f + 0.5f;
            float j = 1.0f - (1.0f - MathHelper.sin(i * (float) Math.PI)) * 0.99f;
            j *= j;

            return new float[] {
                    i * i * 0.3f + 0.2f,
                    i * i * 0.7f + 0.2f,
                    i * 0.1f + 0.7f,
                    j
            };
        }
        return null;
    }
}