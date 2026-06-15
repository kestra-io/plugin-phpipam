package io.kestra.plugin.phpipam.ipam.subnet;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubnetTasksTest {

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
        wireMock.stubFor(get(urlEqualTo("/api/myapp/sections/1/subnets/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    "[{\"id\":\"10\",\"subnet\":\"10.0.0.0\",\"mask\":\"24\",\"sectionId\":\"1\"}]"))));

        var task = List.builder()
            .id(UUID.randomUUID().toString())
            .type(List.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .sectionId(Property.ofValue("1"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getSubnets(), hasSize(1));
        assertThat(output.getSubnets().getFirst().getId(), is("10"));
        assertThat(output.getSubnets().getFirst().getSubnet(), is("10.0.0.0"));
        assertThat(output.getTotal(), is(1));
    }

    @Test
    void get_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/10/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody(
                    "{\"id\":\"10\",\"subnet\":\"10.0.0.0\",\"mask\":\"24\",\"sectionId\":\"1\",\"description\":\"Test\"}"))));

        var task = Get.builder()
            .id(UUID.randomUUID().toString())
            .type(Get.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getSubnet().getId(), is("10"));
        assertThat(output.getSubnet().getDescription(), is("Test"));
    }

    @Test
    void create_returns_new_id() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api/myapp/subnets/"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.createBody("8", "\"192.168.1.0/24\""))));

        var task = Create.builder()
            .id(UUID.randomUUID().toString())
            .type(Create.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnet(Property.ofValue("192.168.1.0"))
            .mask(Property.ofValue("24"))
            .sectionId(Property.ofValue("1"))
            .resourceDescription(Property.ofValue("Office LAN"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("8"));
    }

    @Test
    void update_sends_patch_request() throws Exception {
        wireMock.stubFor(patch(urlEqualTo("/api/myapp/subnets/10/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("null"))));

        var task = Update.builder()
            .id(UUID.randomUUID().toString())
            .type(Update.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .resourceDescription(Property.ofValue("Updated description"))
            .build();

        task.run(runContext());

        wireMock.verify(1, patchRequestedFor(urlEqualTo("/api/myapp/subnets/10/")));
    }

    @Test
    void delete_sends_delete_request() throws Exception {
        wireMock.stubFor(delete(urlEqualTo("/api/myapp/subnets/10/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("null"))));

        var task = Delete.builder()
            .id(UUID.randomUUID().toString())
            .type(Delete.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        task.run(runContext());

        wireMock.verify(1, deleteRequestedFor(urlEqualTo("/api/myapp/subnets/10/")));
    }

    @Test
    void search_by_cidr() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/cidr/192.168.1.0/24/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    "[{\"id\":\"10\",\"subnet\":\"192.168.1.0\",\"mask\":\"24\",\"sectionId\":\"1\"}]"))));

        var task = Search.builder()
            .id(UUID.randomUUID().toString())
            .type(Search.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .cidr(Property.ofValue("192.168.1.0/24"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getSubnets(), hasSize(1));
        assertThat(output.getSubnets().getFirst().getMask(), is("24"));
    }

    @Test
    void search_invalid_cidr_throws_exception() {
        var task = Search.builder()
            .id(UUID.randomUUID().toString())
            .type(Search.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .cidr(Property.ofValue("not-a-cidr"))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContext()));
    }

    @Test
    void first_free_returns_cidr() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/10/first_subnet/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("\"192.168.1.128/25\""))));

        var task = FirstFree.builder()
            .id(UUID.randomUUID().toString())
            .type(FirstFree.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getCidr(), is("192.168.1.128/25"));
    }
}
