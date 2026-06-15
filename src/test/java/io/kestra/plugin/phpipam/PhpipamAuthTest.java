package io.kestra.plugin.phpipam;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhpipamAuthTest {

    @Inject
    RunContextFactory runContextFactory;

    WireMockServer wireMock;

    @BeforeAll
    void startWireMock() {
        wireMock = WireMockSupport.startServer();
    }

    @AfterAll
    void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
    }

    @Test
    void appToken_auth_injects_header() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/sections/"))
            .withHeader("token", equalTo("my-secret-token"))
            .withHeader("X-App-Token", absent())
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody("[{\"id\":\"1\",\"name\":\"Main\"}]"))));

        var task = io.kestra.plugin.phpipam.ipam.section.List.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.List.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(PhpipamAuthentication.builder()
                .appToken(Property.ofValue("my-secret-token"))
                .build())
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSections(), hasSize(1));
    }

    @Test
    void user_password_auth_obtains_session_token() throws Exception {
        // Stub the login endpoint
        wireMock.stubFor(post(urlEqualTo("/api/myapp/user/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("{\"token\":\"session-tok\",\"expires\":\"2099-01-01\"}"))));

        // Stub the actual API call requiring the session token (sent as token header)
        wireMock.stubFor(get(urlEqualTo("/api/myapp/sections/"))
            .withHeader("token", equalTo("session-tok"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody("[{\"id\":\"2\",\"name\":\"Dev\"}]"))));

        var task = io.kestra.plugin.phpipam.ipam.section.List.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.List.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(PhpipamAuthentication.builder()
                .username(Property.ofValue("admin"))
                .password(Property.ofValue("pass"))
                .build())
            .build();

        var output = task.run(runContextFactory.of());
        assertThat(output.getSections(), hasSize(1));
        assertThat(output.getSections().getFirst().getName(), is("Dev"));
    }

    @Test
    void both_auth_modes_throws_validation_error() {
        var task = io.kestra.plugin.phpipam.ipam.section.List.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.List.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(PhpipamAuthentication.builder()
                .appToken(Property.ofValue("tok"))
                .username(Property.ofValue("user"))
                .password(Property.ofValue("pass"))
                .build())
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContextFactory.of()));
    }

    @Test
    void no_auth_mode_throws_validation_error() {
        var task = io.kestra.plugin.phpipam.ipam.section.List.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.List.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(PhpipamAuthentication.builder().build())
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContextFactory.of()));
    }
}
