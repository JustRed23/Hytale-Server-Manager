package dev.JustRed23.hsm.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.JustRed23.hsm.Main;

import java.io.*;
import java.net.http.HttpClient;

import static java.time.Instant.now;
import static java.time.Instant.ofEpochSecond;

public final class Auth {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CREDS_FILE = new File(".hytale-downloader-credentials.json");
    public static Credentials creds;

    public static boolean attemptLogin() {
        creds = loadCredentials();
        boolean result = true;

        if (creds == null || creds.accessToken.isBlank() || creds.refreshToken.isBlank()) {
            Main.LOGGER.warn("No valid credentials found, please log in.");
            result = refreshTokens(false);
        }

        if (now().isAfter(ofEpochSecond(creds.expiresAt))) {
            Main.LOGGER.info("Access token has expired, refreshing...");
            result = refreshTokens(true);
        }

        if (result) Main.LOGGER.info("Successfully authenticated user.");
        return result;
    }

    private static Credentials loadCredentials() {
        if (!CREDS_FILE.exists()) return null;

        try (Reader reader = new FileReader(CREDS_FILE)) {
            return GSON.fromJson(reader, Credentials.class);
        } catch (IOException ioe) {
            Main.LOGGER.error("Could not read credentials file", ioe);
            boolean _ = CREDS_FILE.delete();
            return null;
        }
    }

    private static boolean refreshTokens(boolean canUseRefreshToken) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            TokenRefresher refresher = new TokenRefresher(client, canUseRefreshToken ? creds.refreshToken : null);
            creds = refresher.refreshTokens();

            if (creds == null) {
                Main.LOGGER.error("Failed to refresh tokens.");
                return false;
            }

            try (Writer writer = new FileWriter(CREDS_FILE)) {
                GSON.toJson(creds, writer);
            }

            return true;
        } catch (Exception e) {
            Main.LOGGER.error("Could not refresh tokens", e);
            return false;
        }
    }
}
