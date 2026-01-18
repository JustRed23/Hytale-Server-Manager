package dev.JustRed23.hsm;

import dev.JustRed23.hsm.auth.Auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public final class HttpUtil {

    public static HttpRequest.Builder requestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url));
    }

    public static HttpRequest.Builder authorizedRequestBuilder(String url) {
        return requestBuilder(url).header("Authorization", "Bearer " + Auth.creds.accessToken);
    }

    public static <T> HttpResponse<T> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, bodyHandler);
        }
    }

    public static HttpResponse<String> sendRequest(HttpRequest request) throws Exception {
        return sendRequest(request, HttpResponse.BodyHandlers.ofString());
    }

    public static String getRequestBody(HttpRequest request) throws Exception {
        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() != 200)
            throw new Exception("Request failed (" + response.statusCode() + "): " + response.body());
        return response.body();
    }

    public static HttpRequest downloaderRequest(String url, String body) {
        final String base = "client_id=hytale-downloader&";
        final String fullBody = base + (body == null || body.isBlank() ? "" : "&" + body);
        return requestBuilder(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(fullBody))
                .build();
    }

    public static void downloadFile(String url, File targetFile) throws Exception {
        Files.deleteIfExists(targetFile.toPath());
        HttpRequest request = requestBuilder(url).GET().build();
        HttpResponse<Path> response = sendRequest(request, HttpResponse.BodyHandlers.ofFile(targetFile.toPath()));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(targetFile.toPath());
            throw new Exception("Download of file '" + targetFile.getName() + "' failed (" + response.statusCode() + "): " + response.body());
        }
    }

    public static void downloadAndVerifyFile(String url, File targetFile, String expectedSha256) throws Exception {
        downloadFile(url, targetFile);
        if (!verifyFile(targetFile, expectedSha256)) {
            Files.deleteIfExists(targetFile.toPath());
            throw new Exception("Downloaded file '" + targetFile.getName() + "' failed verification.");
        }
    }

    public static boolean verifyFile(File file, String expectedSha256) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesCount;
                while ((bytesCount = is.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(expectedSha256);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to compute checksum for file: {}", file.getAbsolutePath(), e);
            return false;
        }
    }
}
