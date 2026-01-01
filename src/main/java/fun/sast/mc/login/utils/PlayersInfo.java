package fun.sast.mc.login.utils;

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static fun.sast.mc.login.config.Config.gson;
import static fun.sast.mc.login.config.Config.players;

public class PlayersInfo {
    private static final ConcurrentHashMap<UUID, User> map = new ConcurrentHashMap<>();
    private static final Type type = new TypeToken<ArrayList<User>>() {}.getType();

    public static User get(UUID uuid) {
        return map.get(uuid);
    }

    public static void put(UUID uuid, User user) {
        map.put(uuid, user);
        saveInfo();
    }

    public static boolean exist(UUID uuid) {
        return map.containsKey(uuid);
    }

    public static void initialize() {
        try {
            if (!players.getParentFile().exists()) {
                if (!players.getParentFile().mkdirs()) {
                    throw new RuntimeException("Failed to create parent directories");
                }
            }
            if (!players.exists()) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(players), StandardCharsets.UTF_8)) {
                    writer.write("[]");
                }
            }
            try (BufferedReader bufferedReader = Files.newReader(players, StandardCharsets.UTF_8)) {
                List<User> users = gson.fromJson(bufferedReader, type);
                map.clear();
                if (users != null) {
                    map.putAll(users.stream().collect(Collectors.toMap(User::getUuid, player -> player)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveInfo() {
        try (BufferedWriter bufferedWriter = Files.newWriter(players, StandardCharsets.UTF_8)) {
            List<User> users = new ArrayList<>(map.values().stream().toList());
            bufferedWriter.write(gson.toJson(users));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
