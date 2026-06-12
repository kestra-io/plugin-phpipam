package io.kestra.plugin.phpipam.ipam;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
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
class AddressTasksTest {

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
    void addressList_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/10/addresses/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    "[{\"id\":\"1\",\"subnetId\":\"10\",\"ip\":\"10.0.0.5\",\"hostname\":\"host1\"}]"))));

        var task = AddressList.builder()
            .id(UUID.randomUUID().toString())
            .type(AddressList.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getAddresses(), hasSize(1));
        assertThat(output.getAddresses().getFirst().getIp(), is("10.0.0.5"));
    }

    @Test
    void addressCreate_returns_new_id() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api/myapp/addresses/"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.createBody("99", "\"10.0.0.50\""))));

        var task = AddressCreate.builder()
            .id(UUID.randomUUID().toString())
            .type(AddressCreate.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .ip(Property.ofValue("10.0.0.50"))
            .hostname(Property.ofValue("web01.example.com"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("99"));
    }

    @Test
    void addressFirstFree_returns_ip() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/10/first_free/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("\"10.0.0.2\""))));

        var task = AddressFirstFree.builder()
            .id(UUID.randomUUID().toString())
            .type(AddressFirstFree.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getIp(), is("10.0.0.2"));
    }

    @Test
    void addressGet_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/addresses/42/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody(
                    "{\"id\":\"42\",\"subnetId\":\"10\",\"ip\":\"10.0.0.42\",\"hostname\":\"srv42\"}"))));

        var task = AddressGet.builder()
            .id(UUID.randomUUID().toString())
            .type(AddressGet.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .addressId(Property.ofValue("42"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getAddress().getIp(), is("10.0.0.42"));
        assertThat(output.getAddress().getHostname(), is("srv42"));
    }

    @Test
    void addressDelete_sends_delete_request() throws Exception {
        wireMock.stubFor(delete(urlEqualTo("/api/myapp/addresses/42/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("null"))));

        var task = AddressDelete.builder()
            .id(UUID.randomUUID().toString())
            .type(AddressDelete.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .addressId(Property.ofValue("42"))
            .build();

        task.run(runContext());

        wireMock.verify(1, deleteRequestedFor(urlEqualTo("/api/myapp/addresses/42/")));
    }
}
