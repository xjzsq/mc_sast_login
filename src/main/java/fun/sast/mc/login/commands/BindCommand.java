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
            source.sendMessage(Text.literal("Invalid token: empty"));
            return 0;
        }

        JwtVerifier.Result res = verifier.verify(token);
        if (!res.ok) {
            source.sendMessage(Text.literal("Token error: " + res.error));
            return 0;
        }

        DecodedJWT jwt = res.jwt;
        String uuidClaim = jwt.getClaim("uuid").asString();
        String type = jwt.getClaim("type").asString();
        String userId = jwt.getClaim("user_id").asString();
        String name = jwt.getClaim("name").asString();

        if (uuidClaim == null || type == null || userId == null) {
            source.sendMessage(Text.literal("Invalid token: missing required claims (uuid,type,user_id)"));
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
        if (!playerUuid.toString().equals(uuidClaim)) {
            source.sendMessage(Text.literal("Token UUID does not match your player UUID"));
            return 0;
        }

        if (((PlayerAuth) player).sastLogin$isAuthenticated()) {
            source.sendMessage(Text.literal("You are already authenticated, no need to bind again"));
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
                source.sendMessage(Text.literal("Unknown token type: " + type));
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
        source.sendMessage(Text.literal("Bind successful: linked to " + type + " account " + name + " (id: " + userId + ")"));
        return 1;
    }
}
