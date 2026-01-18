package dev.JustRed23.hsm;

import dev.JustRed23.hsm.auth.Auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpUtil {

    public static HttpRequest.Builder requestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url));
    }

    public static HttpRequest.Builder authorizedRequestBuilder(String url) {
        return requestBuilder(url).header("Authorization", "Bearer " + Auth.creds.accessToken);
    }

    public static HttpResponse<String> sendRequest(HttpRequest request) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    public static String getRequestBody(HttpRequest request) throws Exception {
        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() != 200)
            throw new Exception("Request failed (" + response.statusCode() + "): " + response.body());
        return response.body();
    }
}
