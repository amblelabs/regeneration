package dev.amble.ars.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import dev.amble.ars.data.Attachments;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * 重生指令处理器
 * 提供玩家重生状态查询、设置、触发及时间领主身份管理
 */
public class RegenCommand {

    private static final int PERMISSION_SELF = 0;
    private static final int PERMISSION_ADMIN = 2;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("regen")
                .requires(source -> source.hasPermissionLevel(PERMISSION_SELF))
                .then(literal("get")
                        .executes(ctx -> executeGet(ctx, resolveSelfPlayer(ctx)))
                )
                .then(literal("set")
                        .then(argument("count", IntegerArgumentType.integer(0, RegenerationCore.MAX_REGENERATIONS))
                                .executes(ctx -> executeSet(ctx, resolveSelfPlayer(ctx), IntegerArgumentType.getInteger(ctx, "count")))
                        )
                )
                .then(literal("fix")
                        .executes(ctx -> executeFix(ctx, resolveSelfPlayer(ctx)))
                )
                .then(literal("settimelord")
                        .requires(source -> source.hasPermissionLevel(PERMISSION_ADMIN))
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> executeSetTimelord(ctx, resolveTargetPlayer(ctx, "target")))
                        )
                        .executes(ctx -> executeSetTimelord(ctx, resolveSelfPlayer(ctx)))
                )
                .then(literal("detimelord")
                        .requires(source -> source.hasPermissionLevel(PERMISSION_ADMIN))
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> executeDetimelord(ctx, resolveTargetPlayer(ctx, "target")))
                        )
                        .executes(ctx -> executeDetimelord(ctx, resolveSelfPlayer(ctx)))
                )
                .executes(ctx -> executeTrigger(ctx, resolveSelfPlayer(ctx)))
        );
    }

    // ==================== 执行逻辑 ====================

    private static int executeGet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (player == null) return 0;
        RegenerationCore info = getInfoOrError(ctx, player);
        if (info == null) return 0;

        ctx.getSource().sendFeedback(() ->
                Text.translatable("gui.regen.settings.remaining", info.getUsesLeft()), false);
        return 1;
    }

    private static int executeSet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, int count) {
        if (player == null) return 0;
        RegenerationCore info = getInfoOrError(ctx, player);
        if (info == null) return 0;

        info.setUsesLeft(count);
        ctx.getSource().sendFeedback(() ->
                Text.translatable("gui.regen.settings.remaining", info.getUsesLeft()), false);
        return 1;
    }

    private static int executeFix(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (player == null) return 0;
        RegenerationCore info = getInfoOrError(ctx, player);
        if (info == null) return 0;

        info.stopRegeneration(player);
        ctx.getSource().sendFeedback(() -> Text.translatable("command.regen.stopped"), false);
        return 1;
    }

    private static int executeSetTimelord(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (player == null) return 0;

        RegenerationCapable capable = asCapableOrError(ctx, player);
        if (capable == null) return 0;

        if (!capable.isTimelord()) {
            capable.setTimelord(true);
        }

        // 确保 RegenerationInfo attachment 存在
        RegenerationCore info = capable.getRegenerationInfo();
        if (info != null) {
            info.setUsesLeft(RegenerationCore.MAX_REGENERATIONS);
            info.markDirty();
        }

        // 同步 isTimelord 状态到客户端
        player.setAttached(Attachments.IS_TIMELORD, true);

        ctx.getSource().sendFeedback(() ->
                Text.translatable("command.regen.settimelord.success", player.getName().getString()), true);
        return 1;
    }

    private static int executeDetimelord(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (player == null) return 0;

        RegenerationCapable capable = asCapableOrError(ctx, player);
        if (capable == null) return 0;

        // 1. 停止任何正在进行的再生
        RegenerationCore info = capable.getRegenerationInfo();
        if (info != null) {
            info.stopRegeneration(player);
        }

        // 2. 移除时间领主身份标记
        if (capable.isTimelord()) {
            capable.setTimelord(false);
        }

        // 3. 彻底移除 RegenerationInfo attachment
        // 这样客户端不会再收到同步数据，UI 自然显示无数据
        player.removeAttached(Attachments.REGENERATION);

        // 4. 设置 IS_TIMELORD attachment 为 false，同步到客户端
        player.setAttached(Attachments.IS_TIMELORD, false);

        // 5. 广播同步包，确保所有客户端更新
        if (info != null) {
            info.markDirty();
        }

        ctx.getSource().sendFeedback(() ->
                Text.translatable("command.regen.detimelord.success", player.getName().getString()), true);
        return 1;
    }

    private static int executeTrigger(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (player == null) return 0;
        RegenerationCore info = getInfoOrError(ctx, player);
        if (info == null) return 0;

        if (info.tryStart(player)) {
            ctx.getSource().sendFeedback(() -> Text.translatable("command.regen.triggered"), false);
        } else {
            ctx.getSource().sendError(Text.translatable("command.regen.fail"));
        }
        return 1;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取执行者自身玩家
     */
    private static ServerPlayerEntity resolveSelfPlayer(CommandContext<ServerCommandSource> ctx) {
        try {
            return ctx.getSource().getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    /**
     * 从指令参数解析目标玩家
     */
    private static ServerPlayerEntity resolveTargetPlayer(CommandContext<ServerCommandSource> ctx, String argName) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(ctx, argName);
        if (entity instanceof ServerPlayerEntity player) {
            return player;
        }
        throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
    }

    /**
     * 获取玩家再生信息，失败时自动发送错误消息
     */
    private static RegenerationCore getInfoOrError(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        RegenerationCore info = RegenerationCore.get(player);
        if (info == null) {
            ctx.getSource().sendError(Text.translatable("command.regen.data.error"));
        }
        return info;
    }

    /**
     * 将玩家转为 RegenerationCapable，失败时发送错误消息
     */
    private static RegenerationCapable asCapableOrError(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (player instanceof RegenerationCapable capable) {
            return capable;
        }
        ctx.getSource().sendError(Text.translatable("command.regen.data.error"));
        return null;
    }
}