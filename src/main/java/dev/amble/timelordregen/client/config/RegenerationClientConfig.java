package dev.amble.timelordregen.client.config;

import dev.amble.timelordregen.RegenerationMod;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class RegenerationClientConfig {
    public static final ConfigClassHandler<RegenerationClientConfig> INSTANCE = ConfigClassHandler.createBuilder(RegenerationClientConfig.class)
            .id(RegenerationMod.id("client"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("timelordregen-client.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry public float particles = 1.0f;
    @SerialEntry public float beamScale = 1.0f;
    @SerialEntry public float shake = 1.0f;
    @SerialEntry public boolean vignette = true;
    @SerialEntry public boolean glare = true;
    @SerialEntry public boolean handGlow = true;
    @SerialEntry public HudMode hud = HudMode.ACTIVE;

    public static RegenerationClientConfig get() {
        return INSTANCE.instance();
    }

    public enum HudMode {
        ALWAYS,
        ACTIVE,
        HIDDEN
    }
}
