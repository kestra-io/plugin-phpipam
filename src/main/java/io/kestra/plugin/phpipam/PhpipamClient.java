package io.kestra.plugin.phpipam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Thin HTTP wrapper for the phpIPAM REST API.
 * <p>
 * Uses {@code java.net.http.HttpClient} instead of Kestra's internal HTTP client because
 * self-hosted phpIPAM instances commonly use self-signed TLS certificates, and the trust-all
 * SSL context option requires direct access to the {@link SSLContext} at client construction
 * time — something the Kestra internal client does not expose.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Prepends the base URL + {@code /api/{appId}/} prefix to every path.</li>
 *   <li>Injects the resolved auth header (static App token or user session token).</li>
 *   <li>Unwraps the phpIPAM JSON envelope and raises {@link PhpipamApiException}
 *       on {@code success:false} or non-2xx HTTP status codes.</li>
 *   <li>Optionally trusts all TLS certificates for self-signed / self-hosted instances.</li>
 * </ul>
 * One client instance is created per task run and should not be shared across runs.
 */
public class PhpipamClient {

    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    private final HttpClient httpClient;
    private final String basePrefix;
    private final String resolvedToken;
    private final boolean isAppToken;

    /**
     * @param baseUrl     root URL of the phpIPAM instance, e.g. {@code https://ipam.example.com}
     * @param appId       application ID segment, e.g. {@code myapp}
     * @param token       resolved authentication token (App token or session token)
     * @param isAppToken  {@code true} → use {@code X-App-Token} header;
     *                    {@code false} → use {@code Token} header (session token)
     * @param insecureTls {@code true} to bypass TLS certificate validation
     */
    public PhpipamClient(String baseUrl, String appId, String token,
                         boolean isAppToken, boolean insecureTls) throws Exception {
        this.basePrefix = stripTrailingSlash(baseUrl) + "/api/" + appId;
        this.resolvedToken = token;
        this.isAppToken = isAppToken;

        var builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30));
        if (insecureTls) {
            builder.sslContext(trustAllSslContext());
        }
        this.httpClient = builder.build();
    }

    /**
     * Acquires a session token from {@code POST /api/{appId}/user/} using Basic auth.
     * Returns the token string only; callers construct the client afterwards.
     */
    public static String acquireSessionToken(String baseUrl, String appId,
                                             String username, String password,
                                             boolean insecureTls) throws Exception {
        var prefix = stripTrailingSlash(baseUrl) + "/api/" + appId + "/user/";
        var credentials = Base64.getEncoder().encodeToString(
            (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        var builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30));
        if (insecureTls) {
            builder.sslContext(trustAllSslContext());
        }

        try (var client = builder.build()) {
            var request = HttpRequest.newBuilder(URI.create(prefix))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var envelope = MAPPER.readValue(response.body(),
                new TypeReference<PhpipamEnvelope<Map<String, Object>>>() {});

            if (!envelope.isSuccess()) {
                throw new PhpipamApiException(envelope.getCode(),
                    "Failed to acquire session token: " + envelope.getMessage());
            }

            var data = envelope.getData();
            var token = data.get("token");
            if (token == null) {
                throw new PhpipamApiException(200, "Session token missing from login response");
            }
            return token.toString();
        }
    }

    public <T> T get(String path, TypeReference<PhpipamEnvelope<T>> type) throws Exception {
        var request = baseRequest(path).GET().build();
        return send(request, type);
    }

    public <T> T post(String path, Object body, TypeReference<PhpipamEnvelope<T>> type) throws Exception {
        var json = MAPPER.writeValueAsString(body);
        var request = baseRequest(path)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return send(request, type);
    }

    /**
     * POST for resource creation: returns the top-level {@code id} from the response envelope.
     * phpIPAM places the new resource's numeric id there on HTTP 201, not inside {@code data}.
     */
    public String postCreate(String path, Object body) throws Exception {
        var json = MAPPER.writeValueAsString(body);
        var request = baseRequest(path)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        var envelope = sendRaw(request, new TypeReference<PhpipamEnvelope<Object>>() {});
        return envelope.getId();
    }

    public <T> T patch(String path, Object body, TypeReference<PhpipamEnvelope<T>> type) throws Exception {
        var json = MAPPER.writeValueAsString(body);
        var request = baseRequest(path)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
            .build();
        return send(request, type);
    }

    public void delete(String path) throws Exception {
        var request = baseRequest(path).DELETE().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new PhpipamApiException(404, "Resource not found at " + path);
        }
        var envelope = MAPPER.readValue(response.body(),
            new TypeReference<PhpipamEnvelope<Object>>() {});
        if (!envelope.isSuccess()) {
            throw new PhpipamApiException(envelope.getCode(), envelope.getMessage());
        }
    }

    private HttpRequest.Builder baseRequest(String path) {
        var uri = URI.create(basePrefix + "/" + stripLeadingSlash(path));
        var builder = HttpRequest.newBuilder(uri)
            .header("Content-Type", "application/json");
        if (isAppToken) {
            builder.header("X-App-Token", resolvedToken);
        } else {
            builder.header("Token", resolvedToken);
        }
        return builder;
    }

    private <T> T send(HttpRequest request,
                       TypeReference<PhpipamEnvelope<T>> type) throws Exception {
        return sendRaw(request, type).getData();
    }

    private <T> PhpipamEnvelope<T> sendRaw(HttpRequest request,
                                            TypeReference<PhpipamEnvelope<T>> type) throws Exception {
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();

        if (status == 404) {
            throw new PhpipamApiException(404, "Resource not found: " + request.uri().getPath());
        }
        if (status < 200 || status >= 300) {
            throw new PhpipamApiException(status, "Unexpected HTTP status " + status
                + " for " + request.uri().getPath());
        }

        var envelope = MAPPER.<PhpipamEnvelope<T>>readValue(response.body(), type);
        if (!envelope.isSuccess()) {
            throw new PhpipamApiException(envelope.getCode(), envelope.getMessage());
        }
        return envelope;
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String stripLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private static SSLContext trustAllSslContext() throws Exception {
        var trustAll = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        var ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{trustAll}, new java.security.SecureRandom());
        return ctx;
    }
}
