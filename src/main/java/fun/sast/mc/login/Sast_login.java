package fun.sast.mc.login;

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import fun.sast.mc.login.utils.PlayerAuth;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static fun.sast.mc.login.config.Config.players;
import static fun.sast.mc.login.config.Config.gson;

public class Sast_login implements ModInitializer {
	public static final String MOD_ID = "sast_login";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final ConcurrentHashMap<UUID, User> map = new ConcurrentHashMap<>();
    private static List<User> users;
    private static final Type type = new TypeToken<ArrayList<User>>() {}.getType();


	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        loadInfo();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID uuid = player.getUuid();
            if (map.get(uuid) == null) {
                player.changeGameMode(GameMode.SPECTATOR);
                ((PlayerAuth) player).sastLogin$sendAuthMessage();
            } else {
                ((PlayerAuth) player).sastLogin$setAuthenticated(true);
            }
        });

//        PlayerBlockBreakEvents.BEFORE.register((world, playerEntity, blockPos, blockState, blockEntity) -> {
//            if (!((PlayerAuth) playerEntity).sastLogin$isAuthenticated()) {
//                ((PlayerAuth) playerEntity).sastLogin$sendAuthMessage();
//                return false;
//            }
//            return true;
//        });
//        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
//            if (!((PlayerAuth) playerEntity).sastLogin$isAuthenticated()) {
//                ((PlayerAuth) playerEntity).sastLogin$sendAuthMessage();
//                return ActionResult.FAIL;
//            }
//            return ActionResult.PASS;
//        });
//        UseItemCallback.EVENT.register((playerEntity, world, hand) -> {
//            if (!((PlayerAuth) playerEntity).sastLogin$isAuthenticated()) {
//                ((PlayerAuth) playerEntity).sastLogin$sendAuthMessage();
//                return TypedActionResult.fail(ItemStack.EMPTY);
//            }
//            return TypedActionResult.pass(playerEntity.getStackInHand(hand));
//        });
//        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
//            if (!((PlayerAuth) playerEntity).sastLogin$isAuthenticated()) {
//                ((PlayerAuth) playerEntity).sastLogin$sendAuthMessage();
//                return ActionResult.FAIL;
//            }
//            return ActionResult.PASS;
//        });
//        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
//            if (!((PlayerAuth) playerEntity).sastLogin$isAuthenticated()) {
//                ((PlayerAuth) playerEntity).sastLogin$sendAuthMessage();
//                return ActionResult.FAIL;
//            }
//            return ActionResult.PASS;
//        });

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
                    if (!map.containsKey(user.getUuid())) {
                        map.put(user.getUuid(), user);
                        users.add(user);
                        saveInfo();
                        LOGGER.info("User {} has been created", user.getUuid());
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(user.getUuid());
                        if (player != null) {
                            ((PlayerAuth) player).sastLogin$sendAuthOKMessage();
                            ((PlayerAuth) player).sastLogin$setAuthenticated(true);
                            player.changeGameMode(GameMode.SURVIVAL);
                        }
                    }

                    String response = "ok";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                });
                _server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void loadInfo() {
        try (BufferedReader bufferedReader = Files.newReader(players, StandardCharsets.UTF_8)) {
            users = gson.fromJson(bufferedReader, type);
            map.clear();
            if (users != null) {
                map.putAll(users.stream().collect(Collectors.toMap(User::getUuid, player -> player)));
            } else {
                users = new ArrayList<>();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveInfo() {
        try (BufferedWriter bufferedWriter = Files.newWriter(players, StandardCharsets.UTF_8)) {
            users.clear();
            users.addAll(map.values().stream().toList());
            bufferedWriter.write(gson.toJson(users));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}