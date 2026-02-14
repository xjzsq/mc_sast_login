package fun.sast.mc.login.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.UUID;

import static fun.sast.mc.login.Sast_login.MOD_ID;

public class MojangApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static UUID getUuid(String username) throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + username).toURL().openConnection();
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setConnectTimeout(5000);
        httpsURLConnection.setReadTimeout(5000);

        int response = httpsURLConnection.getResponseCode();
        if (response == HttpURLConnection.HTTP_OK) {
            String responseBody = new String(httpsURLConnection.getInputStream().readAllBytes());
            httpsURLConnection.disconnect();

            // Extract UUID from the response body
            String uuidString = responseBody.split("\"id\" : \"")[1].split("\"")[0];
            LOGGER.debug("Player {} has UUID: {}", username, uuidString);
            return UUID.fromString(uuidString.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
        } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
            httpsURLConnection.disconnect();
            LOGGER.debug("Player {} not found", username);
            return null;
        }
        LOGGER.debug("Unexpected response code {} for player {}", response, username);
        throw new IOException("Unexpected response code " + response + " for player " + username);
    }
}
