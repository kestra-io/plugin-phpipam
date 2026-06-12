package io.kestra.plugin.phpipam.ipam.vlan;

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
class VlanTasksTest {

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
    void list_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/vlan/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    "[{\"vlanId\":\"5\",\"name\":\"MGMT\",\"number\":\"100\"}]"))));

        var task = List.builder()
            .id(UUID.randomUUID().toString())
            .type(List.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .build();

        var output = task.run(runContext());

        assertThat(output.getVlans(), hasSize(1));
        assertThat(output.getVlans().getFirst().getName(), is("MGMT"));
        assertThat(output.getVlans().getFirst().getNumber(), is("100"));
    }

    @Test
    void get_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/vlan/5/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody(
                    "{\"vlanId\":\"5\",\"name\":\"MGMT\",\"number\":\"100\"}"))));

        var task = Get.builder()
            .id(UUID.randomUUID().toString())
            .type(Get.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .vlanId(Property.ofValue("5"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getVlan().getName(), is("MGMT"));
        assertThat(output.getVlan().getNumber(), is("100"));
    }

    @Test
    void create_returns_new_id() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api/myapp/vlan/"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.createBody("7", "\"MGMT\""))));

        var task = Create.builder()
            .id(UUID.randomUUID().toString())
            .type(Create.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .name(Property.ofValue("MGMT"))
            .number(Property.ofValue("100"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("7"));
    }

    @Test
    void update_sends_patch_request() throws Exception {
        wireMock.stubFor(patch(urlEqualTo("/api/myapp/vlan/5/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("null"))));

        var task = Update.builder()
            .id(UUID.randomUUID().toString())
            .type(Update.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .vlanId(Property.ofValue("5"))
            .resourceDescription(Property.ofValue("Updated VLAN description"))
            .build();

        task.run(runContext());

        wireMock.verify(1, patchRequestedFor(urlEqualTo("/api/myapp/vlan/5/")));
    }

    @Test
    void delete_sends_delete_request() throws Exception {
        wireMock.stubFor(delete(urlEqualTo("/api/myapp/vlan/5/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("null"))));

        var task = Delete.builder()
            .id(UUID.randomUUID().toString())
            .type(Delete.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .vlanId(Property.ofValue("5"))
            .build();

        task.run(runContext());

        wireMock.verify(1, deleteRequestedFor(urlEqualTo("/api/myapp/vlan/5/")));
    }
}
