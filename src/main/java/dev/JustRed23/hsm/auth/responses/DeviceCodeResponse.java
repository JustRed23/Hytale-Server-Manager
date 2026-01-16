package dev.JustRed23.hsm.auth.responses;

import com.google.gson.annotations.SerializedName;

public class DeviceCodeResponse {
    public @SerializedName("Header") String header;
    public @SerializedName("device_code") String deviceCode;
    public @SerializedName("user_code") String userCode;
    public @SerializedName("verification_uri") String verificationUri;
    public @SerializedName("verification_uri_complete") String verificationUriComplete;
    public @SerializedName("expires_in") int expiresIn;
    public @SerializedName("interval") int interval;

    @Override
    public String toString() {
        return "DeviceCodeResponse{" +
                "header='" + header + '\'' +
                ", deviceCode='" + deviceCode + '\'' +
                ", userCode='" + userCode + '\'' +
                ", verificationUri='" + verificationUri + '\'' +
                ", verificationUriComplete='" + verificationUriComplete + '\'' +
                ", expiresIn=" + expiresIn +
                ", interval=" + interval +
                '}';
    }
}
