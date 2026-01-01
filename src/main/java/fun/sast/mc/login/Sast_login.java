package fun.sast.mc.login;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import fun.sast.mc.login.utils.PlayerAuth;
import fun.sast.mc.login.utils.PlayersInfo;
import fun.sast.mc.login.utils.User;
import fun.sast.mc.login.commands.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.UUID;

import static fun.sast.mc.login.utils.PremiumChecker.isPremium;

public class Sast_login implements ModInitializer {
	public static final String MOD_ID = "sast_login";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        PlayersInfo.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            BindCommand.registerCommand(dispatcher);
            MigrateCommand.registerCommand(dispatcher);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID uuid = player.getUuid();
            if (PlayersInfo.get(uuid) == null) {
                player.changeGameMode(GameMode.SPECTATOR);
                ((PlayerAuth) player).sastLogin$sendAuthMessage();
            } else {
                player.changeGameMode(GameMode.SURVIVAL);
                ((PlayerAuth) player).sastLogin$setAuthenticated(true);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                HttpServer _server = HttpServer.create(new InetSocketAddress("192.168.114.66",8004), 0);
                _server.createContext("/", exchange -> {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
                    BufferedReader br = new BufferedReader(isr);
                    String request = br.readLine();
                    LOGGER.info("Received request: {}", request);
                    Gson gson = new Gson();
                    User user = gson.fromJson(request, User.class);
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(user.getUuid());
                    if (player != null) {
                        boolean isPremium = isPremium(player);
                        if (!PlayersInfo.exist(user.getUuid()) && isPremium) {
                            PlayersInfo.put(user.getUuid(), user);
                            LOGGER.info("User {} created", user.getUuid());
                        }
                        player.changeGameMode(GameMode.SURVIVAL);
                        if (isPremium) {
                            ((PlayerAuth) player).sastLogin$sendPremiumAuthOKMessage();
                        } else {
                            ((PlayerAuth) player).sastLogin$sendAuthOKMessage();
                        }
                        ((PlayerAuth) player).sastLogin$setAuthenticated(true);
                        String response = "ok";
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } else {
                        String response = "player not online";
                        exchange.sendResponseHeaders(400, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }

                });
                _server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}