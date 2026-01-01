package fun.sast.mc.login.utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.util.UUID;

import static fun.sast.mc.login.integrations.MojangApi.getUuid;

public class PremiumChecker {
    /**
     * 判断玩家是否 premium（正版）
     * @return true = 正版（UUID != Offline UUID），false = 破解
     */
    public static boolean isPremium(ServerPlayerEntity player) throws IOException {
        String username = player.getGameProfile().getName(); // 获取用户名
        UUID onlineUuid = getUuid(username);
        return player.getUuid().equals(onlineUuid);
    }
}