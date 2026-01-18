package dev.JustRed23.hsm.auth;

import dev.JustRed23.hsm.HttpUtil;
import dev.JustRed23.hsm.Main;
import dev.JustRed23.hsm.auth.responses.DeviceCodeResponse;
import dev.JustRed23.hsm.auth.responses.TokenResponse;
import org.jspecify.annotations.Nullable;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class TokenRefresher {

    private final @Nullable String refreshToken;

    public TokenRefresher(@Nullable String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Credentials refreshTokens() throws Exception {
        Credentials refreshTokenCreds = tryRefreshToken();
        if (refreshTokenCreds != null) return refreshTokenCreds;

        HttpRequest request = HttpUtil.downloaderRequest(
                "https://oauth.accounts.hytale.com/oauth2/device/auth",
                "scope=offline+auth:downloader"
        );

        String response = HttpUtil.getRequestBody(request);
        DeviceCodeResponse deviceCodeResponse = Main.GSON.fromJson(response, DeviceCodeResponse.class);

        String message = """
                    
                    =========================================================
                    To authorize this device, please visit the following URL:
                    %s
                    And enter the code: %s
                    
                    Or simply open the following URL in your browser:
                    %s
                    
                    Note: This code will expire in %d seconds.
                    =========================================================
                    """.formatted(
                deviceCodeResponse.verificationUri,
                deviceCodeResponse.userCode,
                deviceCodeResponse.verificationUriComplete,
                deviceCodeResponse.expiresIn
        );
        Main.LOGGER.info(message);

        TokenResponse tokenResponse = waitForUserAuthorization(deviceCodeResponse.deviceCode, deviceCodeResponse.interval, deviceCodeResponse.expiresIn);
        return tokenResponse.toCredentials();
    }

    private Credentials tryRefreshToken() {
        try {
            if (refreshToken != null) {
                HttpRequest request = HttpUtil.downloaderRequest(
                        "https://oauth.accounts.hytale.com/oauth2/token",
                        "grant_type=refresh_token&refresh_token=" + refreshToken
                );

                String response = HttpUtil.getRequestBody(request);
                TokenResponse tokenResponse = Main.GSON.fromJson(response, TokenResponse.class);
                return tokenResponse.toCredentials();
            }
        } catch (Exception e) {
            Main.LOGGER.warn("Failed to refresh tokens using refresh token, falling back to device authorization", e);
        }
        return null;
    }

    private TokenResponse waitForUserAuthorization(String deviceCode, int interval, int expiresIn) {
        int waited = 0;
        while (waited < expiresIn) {
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread was interrupted while waiting for user authorization", e);
            }

            waited += interval;

            HttpRequest request = HttpUtil.downloaderRequest(
                    "https://oauth.accounts.hytale.com/oauth2/token",
                    "grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=" + deviceCode
            );

            try {
                final HttpResponse<String> response = HttpUtil.sendRequest(request);

                if (response.statusCode() == 400 && response.body().contains("authorization_pending"))
                    continue;

                if (response.statusCode() != 200)
                    throw new RuntimeException("Error while polling for device authorization (" + response.statusCode() + "): " + response.body());

                return Main.GSON.fromJson(response.body(), TokenResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Error while polling for device authorization", e);
            }
        }

        throw new RuntimeException("Device code expired before authorization");
    }
}
