package dev.JustRed23.hsm.auth;

import com.google.gson.annotations.SerializedName;

public class Credentials {
    public @SerializedName("access_token") String accessToken;
    public @SerializedName("refresh_token") String refreshToken;
    public @SerializedName("expires_at") long expiresAt;
    public @SerializedName("branch") String branch;

    public String toString() {
        return "Credentials{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expiresAt=" + expiresAt +
                ", branch='" + branch + '\'' +
                '}';
    }
}
