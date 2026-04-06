package ostashko.diploma.zerotrust.demo.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class HttpTestClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    public HttpTestClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public HttpResult get(String path, String bearerToken) {
        return get(path, bearerToken, Map.of());
    }

    public HttpResult get(String path, String bearerToken, Map<String, String> extraHeaders) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        extraHeaders.forEach(builder::header);
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("HTTP request failed", exception);
        }
    }

    public Map<String, Object> json(HttpResult result) {
        try {
            return objectMapper.readValue(result.body(), new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse JSON: " + result.body(), exception);
        }
    }

    public record HttpResult(int status, String body, Map<String, List<String>> headers) {
        public String header(String name) {
            return headers.getOrDefault(name.toLowerCase(), List.of()).stream().findFirst().orElse(null);
        }
    }
}
