package dev.JustRed23.hsm.versioning;

import com.google.gson.JsonObject;
import dev.JustRed23.hsm.HttpUtil;
import dev.JustRed23.hsm.Main;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static dev.JustRed23.hsm.Main.args;

public final class VersionManager {

    private static final File VERSION_DIR = new File("versions");
    private static final File VERISON_MANIFEST_FILE = new File(VERSION_DIR, "versions.json");
    private static final String VERSION_URL_TEMPLATE = "https://account-data.hytale.com/game-assets/version/%s.json";

    private final boolean skipUpdate;
    private final String patchline;
    public VersionManifest manifest;

    public VersionManager(boolean skipUpdate, String patchline) {
        this.skipUpdate = skipUpdate;
        this.patchline = patchline;
    }

    public void run() throws IOException {
        if (!VERSION_DIR.exists() && !VERSION_DIR.mkdirs())
            throw new IOException("Could not create version directory: " + VERSION_DIR.getAbsolutePath());

        boolean manifestExists = VERISON_MANIFEST_FILE.exists();
        if (!manifestExists || !skipUpdate) {
            Main.LOGGER.info("Checking for updates...");
            checkForUpdates(manifestExists);
        }
    }

    private void checkForUpdates(boolean manifestExists) throws IOException {
        if (manifestExists) {
            try (Reader reader = Files.newBufferedReader(VERISON_MANIFEST_FILE.toPath())) {
                manifest = Main.GSON.fromJson(reader, VersionManifest.class);
            }
        } else {
            manifest = new VersionManifest();
        }

        if (manifest.activeVersions == null)
            manifest.activeVersions = new HashMap<>();

        if (manifest.availableVersions == null)
            manifest.availableVersions = new ArrayList<>();

        VersionInfo latestVersionForPatchline = fetchLatestVersionInfo();
        if (latestVersionForPatchline != null) {
            boolean alreadyHaveVersion = manifest.availableVersions.stream()
                    .anyMatch(v -> Objects.equals(v.version, latestVersionForPatchline.version)
                            && Objects.equals(v.patchline, latestVersionForPatchline.patchline));

            if (!alreadyHaveVersion) {
                Main.LOGGER.info("New version '{}' available for patchline {}", latestVersionForPatchline.version, latestVersionForPatchline.patchline);
                manifest.availableVersions.add(latestVersionForPatchline);
                manifest.activeVersions.put(latestVersionForPatchline.patchline, latestVersionForPatchline.version);

                try (Writer writer = Files.newBufferedWriter(VERISON_MANIFEST_FILE.toPath())) {
                    Main.GSON.toJson(manifest, writer);
                }
            } else Main.LOGGER.info("Up to date!");
        }
    }

    private VersionInfo fetchLatestVersionInfo() {
        String latestVersion = String.format(VERSION_URL_TEMPLATE, patchline);
        HttpRequest request = HttpUtil.authorizedRequestBuilder(latestVersion).GET().build();
        try {
            HttpResponse<String> response = HttpUtil.sendRequest(request);
            switch (response.statusCode()) {
                case 200 -> {
                    JsonObject json = Main.GSON.fromJson(response.body(), JsonObject.class);
                    return fetchAssetUrl(json.get("url").getAsString());
                }
                case 403 -> {
                    Main.LOGGER.warn("No access to patchline '{}', cannot check for updates.", patchline);
                    return null;
                }
                default -> {
                    Main.LOGGER.error("Failed to fetch latest version info, server responded with status code: {}", response.statusCode());
                    return null;
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to fetch latest version info", e);
            return null;
        }
    }

    private VersionInfo fetchAssetUrl(String url) {
        HttpRequest request = HttpUtil.requestBuilder(new String(url.getBytes(StandardCharsets.UTF_8))).GET().build();
        try {
            String body = HttpUtil.getRequestBody(request);
            VersionInfo version = Main.GSON.fromJson(body, VersionInfo.class);
            version.patchline = patchline;
            Main.LOGGER.info("Found latest version '{}' for patchline '{}'", version.version, version.patchline);
            return version;
        } catch (Exception e) {
            Main.LOGGER.error("Failed to fetch latest version info from asset URL", e);
            return null;
        }
    }
}
