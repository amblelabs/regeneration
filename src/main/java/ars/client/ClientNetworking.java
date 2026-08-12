package ars.client;

import ars.RegenerationMod;
import ars.api.RegenerationCapable;
import ars.core.RegenerationCore;
import ars.client.gui.RegenerationSettingsScreen;
import ars.data.Attachments;
import ars.network.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class ClientNetworking {

    public static void registerClientReceivers() {
        // 打开 UI
        ClientPlayNetworking.registerGlobalReceiver(Networking.OPEN_GUI_PACKET, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                var player = MinecraftClient.getInstance().player;
                if (player == null) return;

                RegenerationCore info = RegenerationCore.get(player);
                if (info == null) return;

                MinecraftClient.getInstance().setScreen(new RegenerationSettingsScreen(player));
            });
        });

        // 同步数据 —— 增强版：兼容重生期间 world/player 切换
        ClientPlayNetworking.registerGlobalReceiver(RegenerationCore.SYNC_PACKET, (client, handler, buf, responseSender) -> {
            var playerId = buf.readUuid();
            var newInfo = buf.decodeAsJson(RegenerationCore.CODEC);

            client.execute(() -> {
                if (newInfo == null) {
                    RegenerationMod.LOGGER.warn("SYNC_PACKET: null info for {}", playerId);
                    return;
                }

                PlayerEntity entity = null;

                // 优先从 world 查找
                if (client.world != null) {
                    entity = client.world.getPlayerByUuid(playerId);
                }

                // ★ 关键：重生期间 world 可能还没准备好，直接匹配 client.player
                if (entity == null && client.player != null && client.player.getUuid().equals(playerId)) {
                    entity = client.player;
                }

                if (entity == null) {
                    RegenerationMod.LOGGER.warn("SYNC_PACKET: player {} not found (world={}, client.player={})",
                            playerId, client.world != null, client.player != null);
                    return;
                }

                if (!(entity instanceof RegenerationCapable)) {
                    RegenerationMod.LOGGER.warn("SYNC_PACKET: player {} is not RegenerationCapable", playerId);
                    return;
                }

                entity.setAttached(Attachments.REGENERATION, newInfo);
                entity.setAttached(Attachments.IS_TIMELORD, true);

                RegenerationMod.LOGGER.debug("RegenerationInfo synced to client for {}", playerId);
            });
        });
    }
}