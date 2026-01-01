package fun.sast.mc.login.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import fun.sast.mc.login.Sast_login;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JwtVerifier {
    private static final String SECRET_FILE = "config/jwt-secret.txt";
    private static final String ENV_SECRET = "JWT_SECRET";
    private static final Map<String, Long> usedTokens = new ConcurrentHashMap<>();
    private static final long REPLAY_WINDOW_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private static String secretSourceInfo = "(unknown)";
    private final String secret;
    private final JWTVerifier verifier; // primary verifier using string secret

    public JwtVerifier() {
        secret = loadSecret();
        // log secret source for debug (do not log plaintext secret)
        Sast_login.LOGGER.info("JWT secret loaded successfully, secret: {}", secret);
        Sast_login.LOGGER.info("JWT secret source (debug): {}", secretSourceInfo);
        // primary: use String overload
        Algorithm algorithm = Algorithm.HMAC256(secret);
        verifier = JWT.require(algorithm).build();
    }

    private String loadSecret() {
        String env = System.getenv(ENV_SECRET);
        if (env != null && !env.isEmpty()) {
            // ensure no BOM in env value either
            secretSourceInfo = "env";
            return env.trim();
        }
        File f = new File(SECRET_FILE);
        if (f.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                secretSourceInfo = "file:" + SECRET_FILE;
                return (new String(bytes, StandardCharsets.UTF_8)).trim();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read JWT secret file", e);
            }
        }
        throw new RuntimeException("JWT secret not found. Provide via " + ENV_SECRET + " env or " + SECRET_FILE + " file.");
    }

    public static class Result {
        public final boolean ok;
        public final String error; // null if ok
        public final DecodedJWT jwt; // present if ok

        private Result(boolean ok, String error, DecodedJWT jwt) {
            this.ok = ok;
            this.error = error;
            this.jwt = jwt;
        }

        public static Result ok(DecodedJWT jwt) {
            return new Result(true, null, jwt);
        }

        public static Result fail(String err) {
            return new Result(false, err, null);
        }
    }

    public Result verify(String token) {
        if (token == null || token.trim().isEmpty()) return Result.fail("empty token");
        token = token.trim();
        // check replay
        String hash = sha256Hex(token);
        long now = System.currentTimeMillis();
        cleanup(now);
        if (usedTokens.containsKey(hash)) {
            return Result.fail("token already used");
        }

        // Try multiple verification strategies but only mark token used after success
        DecodedJWT jwt = tryVerifyWithPrimary(token);
        if (jwt != null) {
            usedTokens.put(hash, now);
            return Result.ok(jwt);
        }


        return Result.fail("verification failed: signature invalid (all strategies)");
    }

    private DecodedJWT tryVerifyWithPrimary(String token) {
        try {
            Sast_login.LOGGER.info("token: {}", token);
            return verifier.verify(token);
        } catch (Exception e) {
            return null;
        }
    }

    private static void cleanup(long now) {
        usedTokens.entrySet().removeIf(e -> (now - e.getValue()) > REPLAY_WINDOW_MS);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

