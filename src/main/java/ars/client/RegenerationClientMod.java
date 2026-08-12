package ars.client;

import ars.RegenerationMod;
import ars.client.gui.DelayOverlay;
import ars.client.gui.RegenerationSettingsScreen;
import ars.client.particle.RegenHeadParticle;
import ars.client.particle.RightRegenParticle;
import ars.client.renderers.sky.GallifreySkyProperties;
import ars.client.util.ClientColors;
import ars.core.RegenerationDimensions;
import ars.core.RegenerationModBlocks;
import ars.network.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import static ars.RegenerationMod.id;

public class RegenerationClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
		ClientNetworking.registerClientReceivers();

        Animations.init();
        resourcepackRegister();


        DimensionRenderingRegistry.registerDimensionEffects(RegenerationDimensions.GALLIFREY.getValue(), new GallifreySkyProperties());

        // Block render stuff
        blockRenderLayers();

        // Register particles on client side
        registerParticles();

        // Register Grass ColorMap (yes ik i did it awkward- addie)
        ClientColors.registerGallifreyGrassColor();


        BlockRenderLayerMapRegister();

	    HudRenderCallback.EVENT.register(new DelayOverlay());

        ClientPlayNetworking.registerGlobalReceiver(Networking.OPEN_GUI_PACKET, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                if (client.player != null) {
                    client.setScreen(new RegenerationSettingsScreen(client.player));
                }
            });
        });
    registerKeyBindings();
}

    private void registerKeyBindings() {
        // GUI 设置键：默认 U
        KeyBinding openSettingsKey = new KeyBinding(
                "key.timelordregen.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,        // ★ 默认 U 键
                "category.timelordregen"
        );
        KeyBindingHelper.registerKeyBinding(openSettingsKey);

        // 强制重生键：默认未绑定，避免误触；玩家可手动在控制菜单里绑定
        KeyBinding forceRegenKey = new KeyBinding(
                "key.timelordregen.force_regen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,  // ★ 默认无按键
                "category.timelordregen"
        );
        KeyBindingHelper.registerKeyBinding(forceRegenKey);

        // 长按状态
        final int[] holdTicks = {0};
        final boolean[] triggered = {false};

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // GUI 打开（单击触发）
            if (openSettingsKey.wasPressed()) {
                ClientPlayNetworking.send(Networking.REQUEST_OPEN_GUI, PacketByteBufs.create());
            }

            // 强制重生：长按 2s（40 tick）触发
            if (forceRegenKey.isPressed()) {
                holdTicks[0]++;
                if (holdTicks[0] >= 40 && !triggered[0]) {
                    triggered[0] = true;
                    ClientPlayNetworking.send(Networking.FORCE_REGEN, PacketByteBufs.empty());
                    client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 1.0f);
                }
            } else {
                holdTicks[0] = 0;
                triggered[0] = false;
            }
        });
    }

    public static void BlockRenderLayerMapRegister() {
        BlockRenderLayerMap.INSTANCE.putBlock(RegenerationModBlocks.GALLIFREY_GRASS_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(RegenerationModBlocks.FLOWER_OF_REMEMBRANCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(RegenerationModBlocks.POTTED_FLOWER_OF_REMEMBRANCER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(RegenerationModBlocks.MOONLIGHT_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(RegenerationModBlocks.TYPHA_POD, RenderLayer.getCutout());
    }

    public static class Animations {

        public static class Players {
            //public static final Supplier<PlayerAnimationHolder> REGENERATE = AnimationRegistry.instance().register(() -> new PlayerAnimationHolder(new Identifier(RegenerationMod.MOD_ID, "regeneration"), RegenerationAnimations.CASE_OPEN_PLAYER));

            public static void init() {}

        }

        public static void init() {
            Players.init();
        }
    }

    public void blockRenderLayers() {
        BlockRenderLayerMap map = BlockRenderLayerMap.INSTANCE;
        map.putBlock(RegenerationModBlocks.CADON_LEAVES, RenderLayer.getCutout());
        map.putBlock(RegenerationModBlocks.CADON_TRAPDOOR, RenderLayer.getCutout());
        map.putBlock(RegenerationModBlocks.CADON_DOOR, RenderLayer.getCutout());
        map.putBlock(RegenerationModBlocks.CADON_SAPLING, RenderLayer.getCutout());
    }

    public void registerParticles() {
        ParticleFactoryRegistry.getInstance().register(RegenerationMod.RIGHT_REGEN_PARTICLE, RightRegenParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(RegenerationMod.REGEN_HEAD_PARTICLE, RegenHeadParticle.Factory::new);
    }

    public static void resourcepackRegister() {

        // Register builtin resourcepacks
        FabricLoader.getInstance().

                getModContainer("timelordregen").

                ifPresent(modContainer ->

                {
                    ResourceManagerHelper.registerBuiltinResourcePack(id("bushy_leaves"), modContainer, ResourcePackActivationType.NORMAL);
                });
    }
}
