package dev.JustRed23.hsm.versioning;

import com.google.gson.JsonObject;
import dev.JustRed23.hsm.HttpUtil;
import dev.JustRed23.hsm.Main;

import java.io.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class VersionManager {

    private static final File VERSION_DIR = new File("versions");
    private static final File VERISON_MANIFEST_FILE = new File(VERSION_DIR, "versions.json");
    private static final String VERSION_URL_TEMPLATE = "https://account-data.hytale.com/game-assets/version/%s.json";
    private static final String BUILD_URL_TEMPLATE = "https://account-data.hytale.com/game-assets/%s";

    private static final String SERVER_JAR_NAME = "HytaleServer.jar";
    private static final String SERVER_AOT_NAME = "HytaleServer.aot";
    private static final String ASSETS_ZIP_NAME = "Assets.zip";

    private final boolean skipUpdate;
    private final String patchline;
    private VersionManifest manifest;

    private File serverJar, aotFile, assetsZip;

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

        Main.LOGGER.info("Verifying active installation...");
        if (!verifyActiveInstallation()) return;

        Main.LOGGER.info("Launching server...");
        Main.startServer(
                "java",
                "-XX:AOTCache=" + aotFile.getAbsolutePath(),
                "-jar", serverJar.getAbsolutePath(),
                "--assets", assetsZip.getAbsolutePath()
        );
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

    private boolean verifyActiveInstallation() {
        VersionInfo activeVersion = manifest.getForPatchline(patchline);
        if (activeVersion == null) {
            Main.LOGGER.warn("No active version set for patchline '{}'", patchline);
            return false;
        }

        File activeVersionDir = new File(VERSION_DIR, activeVersion.version);
        if (!activeVersionDir.exists() && !activeVersionDir.mkdirs()) {
            Main.LOGGER.error("Could not create directory for active version: {}", activeVersionDir.getAbsolutePath());
            return false;
        }

        File packedZip = new File(activeVersionDir, "Hytale-Server-%s.zip".formatted(activeVersion.version));
        serverJar = new File(activeVersionDir, SERVER_JAR_NAME);
        aotFile = new File(activeVersionDir, SERVER_AOT_NAME);
        assetsZip = new File(activeVersionDir, ASSETS_ZIP_NAME);

        return downloadAndExtract(activeVersion, packedZip, serverJar, aotFile, assetsZip);
    }

    private boolean downloadAndExtract(VersionInfo version, File packedZip, File serverJar, File aotFile, File assetsZip) {
        String buildUrl = String.format(BUILD_URL_TEMPLATE, version.downloadUrl);

        HttpRequest request = HttpUtil.authorizedRequestBuilder(buildUrl).GET().build();

        String assetsZipUrl;
        try {
            String assetsUrl = HttpUtil.getRequestBody(request);
            JsonObject json = Main.GSON.fromJson(assetsUrl, JsonObject.class);
            assetsZipUrl = json.get("url").getAsString();
            assetsZipUrl = new String(assetsZipUrl.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Main.LOGGER.error("Failed to get zip url for version '{}'", version.version, e);
            return false;
        }

        try {
            if (!packedZip.exists() || !HttpUtil.verifyFile(packedZip, version.sha256)) {
                Main.LOGGER.info("Downloading zip file for version '{}'. This might take a while...", version.version);
                HttpUtil.downloadAndVerifyFile(assetsZipUrl, packedZip, version.sha256);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to download zip file for version '{}'", version.version, e);
            return false;
        }

        if (serverJar.exists() && aotFile.exists() && assetsZip.exists()) {
            Main.LOGGER.info("Successfully verified files");
            return true;
        }

        Main.LOGGER.info("Extracting files...");

        try (ZipFile zip = new ZipFile(packedZip)) {
            extractFileFromZip(zip, "Server/HytaleServer.aot", aotFile);
            extractFileFromZip(zip, "Server/HytaleServer.jar", serverJar);
            extractFileFromZip(zip, "Assets.zip", assetsZip);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to extract files from zip", e);
            return false;
        }

        Main.LOGGER.info("Successfully extracted files");
        return true;
    }

    private void extractFileFromZip(ZipFile zip, String fileNameInZip, File targetFile) throws IOException {
        final ZipEntry entry = zip.getEntry(fileNameInZip);
        if (entry == null)
            throw new IOException("File '" + fileNameInZip + "' not found in zip archive.");

        try (
                InputStream is = zip.getInputStream(entry);
                CheckedInputStream cis = new CheckedInputStream(is, new CRC32())
        ) {
            Files.copy(cis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            final long crc = entry.getCrc();
            final long actualCrc = cis.getChecksum().getValue();
            if (crc != -1 && actualCrc != crc)
                throw new IOException("CRC mismatch for " + fileNameInZip);
        }
    }
}
