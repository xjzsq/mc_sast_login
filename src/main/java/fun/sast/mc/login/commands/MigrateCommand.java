package fun.sast.mc.login.commands;

import com.mojang.brigadier.CommandDispatcher;
import fun.sast.mc.login.utils.PlayerDataMigration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static fun.sast.mc.login.utils.PremiumChecker.isPremium;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MigrateCommand {
    public static void registerCommand(CommandDispatcher<ServerCommandSource>  dispatcher) {
        // 使用 /migrate <offline_username> 进行账号迁移
        dispatcher.register(literal("migrate")
                .then(argument("offline_username", string())
                        .executes(ctx -> {
                            try {
                                return migrate(ctx.getSource(), getString(ctx, "offline_username"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ))
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal("Usage: /migrate <offline_username>"));
                    return 0;
                }));
    }

    private static int migrate(ServerCommandSource source, String offlineUsername) throws IOException {
        // 这里添加账号迁移的逻辑
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Command must be run by a player"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("Player not found"));
            return 0;
        }

        if (!isPremium((ServerPlayerEntity) player)) {
            source.sendError(Text.literal("仅限已验证的正版用户使用此命令"));
            return 0;
        }

        final MinecraftServer server = source.getServer();

        if (!PlayerDataMigration.canMigrate(server, offlineUsername)) {
            source.sendError(Text.literal("离线用户名 " + offlineUsername + " 不存在或没有可迁移的数据"));
            return 0;
        }

        Text kickReason = Text.literal("正在进行账号迁移，请稍作等待后重新登录...");

        try {
            if (player.networkHandler != null) {
                player.networkHandler.disconnect(kickReason);
            }
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Failed to disconnect player: " + e.getMessage()), false);
        }

        source.sendFeedback(() -> Text.literal("Migrating data from offline username: " + offlineUsername + " to current account: " +
                //? if < 1.21.5 {
                /*player.getGameProfile().getName())
                *///? } else {
                 player.getName().getString())
                 //?}
                , false);

        final UUID onlineUuid = player.getUuid();
        final AtomicBoolean stillOnline = new AtomicBoolean(true);

        new Thread(() -> {
            final int MAX_WAIT_MS = 10_000; // 最大等待 15 秒
            final int POLL_INTERVAL_MS = 200; // 每 200ms 检查一次
            long start = System.currentTimeMillis();
            boolean observedOnline = true;
            while (System.currentTimeMillis() - start < MAX_WAIT_MS) {
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                final AtomicBoolean isOnlineNow = new AtomicBoolean(true);
                server.execute(() -> {
                    try {
                        isOnlineNow.set(server.getPlayerManager().getPlayer(onlineUuid) != null);
                    } finally {
                        latch.countDown();
                    }
                });
                try {
                    // 等待主线程完成检查（最多等 1 秒）
                    latch.await(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!isOnlineNow.get()) {
                    // 确认玩家已被移除，等额外几 tick 确保保存完成（例如 1 秒）
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    observedOnline = false;
                    break;
                }

                // 仍然在线，短暂等待后重试
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            stillOnline.set(observedOnline);
            server.execute(() -> {
                if (stillOnline.get()) {
                    source.sendFeedback(() -> Text.literal("Player is still online after waiting, proceeding with migration."), false);
                    return;
                }
                try {
                    PlayerDataMigration.migrate(server, onlineUuid, offlineUsername);
                    source.sendFeedback(() -> Text.literal("Account migration for username: " + offlineUsername), false);
                } catch (Exception e) {
                    source.sendFeedback(() -> Text.literal("Account migration failed due to an error: " + e.getMessage()), false);
                }
            });
        }, "sast-login-migrate-waiter-" + onlineUuid.toString()).start();

        return 1;
    }
}
