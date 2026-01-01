package fun.sast.mc.login.utils;

import com.google.common.io.Files;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Uuids;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static fun.sast.mc.login.Sast_login.MOD_ID;

public class PlayerDataMigration {
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean migrate(MinecraftServer server, UUID onlineUuid, String offlineUsername) {
        UUID offlineUuid = Uuids.getOfflinePlayerUuid(offlineUsername);

        File playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        File advancementsDir = server.getSavePath(WorldSavePath.ADVANCEMENTS).toFile();
        File statsDir = server.getSavePath(WorldSavePath.STATS).toFile();

        boolean dataMoved = moveFiles(playerDataDir, offlineUuid.toString(), onlineUuid.toString(), ".dat", "player data");
        boolean advancementsMoved = moveFiles(advancementsDir, offlineUuid.toString(), onlineUuid.toString(), ".json", "advancements");
        boolean statsMoved = moveFiles(statsDir, offlineUuid.toString(), onlineUuid.toString(), ".json", "stats");
        return dataMoved || advancementsMoved || statsMoved;
    }

    public static boolean moveFiles(File parent, String fromUuid, String toUuid, String extName, String kind) {
        File fromFile = new File(parent, fromUuid + extName);
        File toFile = new File(parent, toUuid + extName);
        try {
            if (!fromFile.exists()) {
                LOGGER.info("No offline {} found for {}", kind, fromUuid);
                return false; // 离线数据文件不存在，无法迁移
            }
            if (toFile.exists()) {
                File backupFile = new File(parent, toUuid + "_backup" + extName);
                Files.copy(toFile.toPath().toFile(), backupFile);
                LOGGER.info("Backed up existing online {} for {} to {}", kind, toUuid, backupFile.getName());
            }
            Files.copy(fromFile.toPath().toFile(), toFile.toPath().toFile());
            LOGGER.info("Successfully migrated {} from {} to {}", kind, fromUuid, toUuid);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to migrate {} from {} to {}: {}", kind, fromUuid, toUuid, e.getMessage());
            return false;
        }
    }
}
