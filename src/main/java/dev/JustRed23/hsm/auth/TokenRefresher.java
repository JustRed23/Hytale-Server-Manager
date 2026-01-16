package dev.JustRed23.hsm.auth;

import com.google.gson.Gson;
import dev.JustRed23.hsm.Main;
import dev.JustRed23.hsm.auth.responses.DeviceCodeResponse;
import dev.JustRed23.hsm.auth.responses.TokenResponse;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class TokenRefresher {

    private static final Gson GSON = new Gson();
    private final HttpClient client;
    private final @Nullable String refreshToken;

    public TokenRefresher(HttpClient client, @Nullable String refreshToken) {
        this.client = client;
        this.refreshToken = refreshToken;
    }

    public Credentials refreshTokens() throws RuntimeException {
        if (refreshToken != null) {
            HttpRequest request = baseRequest(
                    URI.create("https://oauth.accounts.hytale.com/oauth2/token"),
                    "grant_type=refresh_token&refresh_token=" + refreshToken
            );

            String response = manageResponse(request);
            TokenResponse tokenResponse = GSON.fromJson(response, TokenResponse.class);
            return tokenResponse.toCredentials();
        }

        HttpRequest request = baseRequest(
                URI.create("https://oauth.accounts.hytale.com/oauth2/device/auth"),
                "scope=offline+auth:downloader"
        );

        String response = manageResponse(request);
        DeviceCodeResponse deviceCodeResponse = GSON.fromJson(response, DeviceCodeResponse.class);

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

    private HttpRequest baseRequest(URI uri, String body) {
        final String base = "client_id=hytale-downloader&";
        final String fullBody = base + (body == null || body.isBlank() ? "" : "&" + body);
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(fullBody))
                .build();
    }

    private String manageResponse(HttpRequest req) throws RuntimeException {
        try {
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new RuntimeException("Request returned invalid repsonse (" + response.statusCode() + "): " + response.body());
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Request returned error", e);
        }
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

            HttpRequest request = baseRequest(
                    URI.create("https://oauth.accounts.hytale.com/oauth2/token"),
                    "grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=" + deviceCode
            );

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 400 && response.body().contains("authorization_pending"))
                    continue;

                if (response.statusCode() != 200)
                    throw new RuntimeException("Error while polling for device authorization (" + response.statusCode() + "): " + response.body());

                return GSON.fromJson(response.body(), TokenResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Error while polling for device authorization", e);
            }
        }

        throw new RuntimeException("Device code expired before authorization");
    }
}
