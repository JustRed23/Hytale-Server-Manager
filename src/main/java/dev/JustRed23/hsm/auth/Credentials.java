package dev.JustRed23.hsm.auth;

public class Credentials {
    public String accessToken;
    public String refreshToken;
    public long expiresAt;
    public String branch;

    public String toString() {
        return "Credentials{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expiresAt=" + expiresAt +
                ", branch='" + branch + '\'' +
                '}';
    }
}
