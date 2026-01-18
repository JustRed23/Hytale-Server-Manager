package dev.JustRed23.hsm.versioning;

import com.google.gson.annotations.SerializedName;

public class VersionInfo {
    public @SerializedName("version") String version;
    public @SerializedName("download_url") String downloadUrl;
    public @SerializedName("sha256") String sha256;
    public @SerializedName("patchline") String patchline;
}
