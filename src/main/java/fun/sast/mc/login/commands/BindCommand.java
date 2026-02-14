package fun.sast.mc.login.commands;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mojang.brigadier.CommandDispatcher;
import fun.sast.mc.login.utils.PlayerAuth;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static fun.sast.mc.login.Sast_login.MOD_ID;
import static fun.sast.mc.login.utils.PremiumChecker.isPremium;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import fun.sast.mc.login.utils.JwtVerifier;
import fun.sast.mc.login.utils.PlayersInfo;
import fun.sast.mc.login.utils.User;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindCommand {
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 通过 /bind <jwt_token> 进行身份验证
        dispatcher.register(literal("bind")
                .then(argument("token", string())
                        .executes(ctx -> {
                            try {
                                return bind(ctx.getSource(), getString(ctx, "token"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ))
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal("Usage: /bind <token>"));
                    return 0;
                }));
    }

    private static final JwtVerifier verifier = new JwtVerifier();

    private static int bind(ServerCommandSource source, String token) throws IOException {
        token = token == null ? "" : token.trim();
        if (token.isEmpty()) {
            source.sendMessage(Text.literal("绑定失败：token 不能为空！"));
            return 0;
        }

        ServerPlayerEntity player;
        try {
            player = source.getPlayer();
        } catch (Exception e) {
            source.sendMessage(Text.literal("Command must be run by a player"));
            return 0;
        }
        if (player == null) {
            source.sendMessage(Text.literal("Command must be run by a player"));
            return 0;
        }

        UUID playerUuid = player.getUuid();
        if (((PlayerAuth) player).sastLogin$isAuthenticated()) {
            LOGGER.info("Player {} is already authenticated, no need to bind again", playerUuid);
            source.sendMessage(Text.literal("绑定失败：已绑定过账号，无需重复绑定"));
            return 0;
        }

        JwtVerifier.Result res = verifier.verify(token);
        if (!res.ok) {
            LOGGER.info("Token error: {}", res.error);
            source.sendMessage(Text.literal("绑定失败：" + res.error));
            return 0;
        }

        DecodedJWT jwt = res.jwt;
        String uuidClaim = jwt.getClaim("uuid").asString();
        String type = jwt.getClaim("type").asString();
        String userId = jwt.getClaim("user_id").asString();
        String name = jwt.getClaim("name").asString();

        if (uuidClaim == null || type == null || userId == null) {
            LOGGER.info("Invalid token: missing required claims (uuid,type,user_id)");
            source.sendMessage(Text.literal("绑定失败：token 缺少必要的字段 (uuid,type,user_id)"));
            return 0;
        }

        if (!playerUuid.toString().equals(uuidClaim)) {
            LOGGER.info("Token UUID does not match player UUID: token {}, player {}", uuidClaim, playerUuid);
            source.sendMessage(Text.literal("绑定失败：token 中的 UUID 与当前玩家不匹配"));
            return 0;
        }

        User user = new User();
        user.setUuid(playerUuid);

        switch (type) {
            case "feishu":
                user.setFeishu_id(userId);
                user.setFeishu_name(name);
                break;
            case "sast":
                user.setSast_id(userId);
                user.setSast_name(name);
                break;
            default:
                LOGGER.info("Invalid token type: {}", type);
                source.sendMessage(Text.literal("绑定失败：未知的 type 字段（"+ type +"）"));
                return 0;
        }

        boolean isPremium = isPremium(player);

        if (!PlayersInfo.exist(playerUuid) && isPremium) {
            PlayersInfo.put(playerUuid, user);
            LOGGER.info("User {} created", user.getUuid());
        }
        player.changeGameMode(GameMode.SURVIVAL);
        if (isPremium) {
            ((PlayerAuth) player).sastLogin$sendPremiumAuthOKMessage();
        } else {
            ((PlayerAuth) player).sastLogin$sendAuthOKMessage();
        }
        ((PlayerAuth) player).sastLogin$setAuthenticated(true);
        source.sendMessage(Text.literal("绑定成功：已绑定到 " + type + " 账号 " + name + "（id: " + userId + "）"));
        return 1;
    }
}
