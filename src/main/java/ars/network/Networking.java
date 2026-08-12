package ars.network;

import ars.core.RegenerationCore;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Networking {
    public static final Identifier OPEN_GUI_PACKET = new Identifier("timelordregen", "open_gui");
    public static final Identifier REQUEST_OPEN_GUI = new Identifier("timelordregen", "request_open_gui");
    public static final Identifier FORCE_REGEN = new Identifier("timelordregen", "force_regen");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_OPEN_GUI, (server, player, handler, buf, responseSender) -> {
            sendOpenGuiPacket(player);
        });

        ServerPlayNetworking.registerGlobalReceiver(FORCE_REGEN, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> handleForceRegen(player));
        });
    }

    private static void handleForceRegen(ServerPlayerEntity player) {
        RegenerationCore info = RegenerationCore.get(player);
        if (info == null || info.getUsesLeft() <= 0) return;

        // 未激活：进入延缓期
        if (!info.isActive()) {
            info.tryStart(player);
            syncRegenInfoToClient(player, info); // ★ 立即同步，不要等 tick
            return;
        }

        // 延缓期中：强制跳过，立即重生
        if (info.getDelay().isRunning()) {
            info.getDelay().stop();
            info.setRegenQueued(true);
            syncRegenInfoToClient(player, info); // ★ 立即同步
        }
    }

    /**
     * 手动构造 SYNC_PACKET 立即推送给指定玩家
     * 解决 markDirty() 延迟同步导致的客户端状态滞后
     */
    private static void syncRegenInfoToClient(ServerPlayerEntity player, RegenerationCore info) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(player.getUuid());
        buf.encodeAsJson(RegenerationCore.CODEC, info);
        ServerPlayNetworking.send(player, RegenerationCore.SYNC_PACKET, buf);
    }

    public static void sendOpenGuiPacket(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN_GUI_PACKET, PacketByteBufs.create());
    }
}