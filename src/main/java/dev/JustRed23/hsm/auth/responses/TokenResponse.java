package dev.JustRed23.hsm.auth.responses;

import com.google.gson.annotations.SerializedName;
import dev.JustRed23.hsm.auth.Credentials;

import java.time.Instant;

import static dev.JustRed23.hsm.Main.args;

public class TokenResponse {
    public @SerializedName("access_token") String accessToken;
    public @SerializedName("expires_in") int expiresIn;
    public @SerializedName("refresh_token") String refreshToken;
    public @SerializedName("scope") String scope;
    public @SerializedName("token_type") String tokenType;

    @Override
    public String toString() {
        return "DeviceCodeTokenResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshToken='" + refreshToken + '\'' +
                ", scope='" + scope + '\'' +
                ", tokenType='" + tokenType + '\'' +
                '}';
    }

    public Credentials toCredentials() {
        Credentials credentials = new Credentials();
        credentials.accessToken = accessToken;
        credentials.refreshToken = refreshToken;
        credentials.expiresAt = Instant.now().getEpochSecond() + expiresIn;
        credentials.branch = args.get(args.patchline);
        return credentials;
    }
}
