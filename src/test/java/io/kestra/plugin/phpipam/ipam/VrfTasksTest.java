package io.kestra.plugin.phpipam.ipam;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.phpipam.PhpipamAuthentication;
import io.kestra.plugin.phpipam.WireMockSupport;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VrfTasksTest {

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

    private PhpipamAuthentication auth() {
        return PhpipamAuthentication.builder()
            .appToken(Property.ofValue("test-token"))
            .build();
    }

    private RunContext runContext() {
        return runContextFactory.of();
    }

    @Test
    void vrfList_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/vrf/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    "[{\"vrfId\":\"2\",\"name\":\"CORP-VRF\",\"rd\":\"65000:1\"}]"))));

        var task = VrfList.builder()
            .id(UUID.randomUUID().toString())
            .type(VrfList.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .build();

        var output = task.run(runContext());

        assertThat(output.getVrfs(), hasSize(1));
        assertThat(output.getVrfs().getFirst().getName(), is("CORP-VRF"));
        assertThat(output.getVrfs().getFirst().getRd(), is("65000:1"));
    }

    @Test
    void vrfCreate_returns_new_id() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api/myapp/vrf/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("\"3\""))));

        var task = VrfCreate.builder()
            .id(UUID.randomUUID().toString())
            .type(VrfCreate.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .name(Property.ofValue("CORP-VRF"))
            .rd(Property.ofValue("65000:1"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("3"));
    }
}
