package io.kestra.plugin.phpipam;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Shared lifecycle helper for WireMock-based phpIPAM API tests.
 * Start one server per test class; reset stubs between tests.
 */
public final class WireMockSupport {

    private WireMockSupport() {}

    public static WireMockServer startServer() {
        var server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        return server;
    }

    /** Base URL that tasks should receive (no trailing slash). */
    public static String baseUrl(WireMockServer server) {
        return "http://localhost:" + server.port();
    }

    /** JSON envelope for a successful response with a single data object. */
    public static String successBody(String dataJson) {
        return """
            {"code":200,"success":true,"data":%s}
            """.formatted(dataJson).strip();
    }

    /** JSON envelope for a list response. */
    public static String successListBody(String dataJson) {
        return successBody(dataJson);
    }

    /** JSON envelope representing an API-level failure (success:false). */
    public static String failureBody(int code, String message) {
        return """
            {"code":%d,"success":false,"message":"%s"}
            """.formatted(code, message).strip();
    }
}
