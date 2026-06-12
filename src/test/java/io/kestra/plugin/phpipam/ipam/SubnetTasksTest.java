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
    void subnetList_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/sections/1/subnets/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    """
                    [{"id":"10","subnet":"10.0.0.0","mask":"24","sectionId":"1"}]
                    """))));

        var task = SubnetList.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetList.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .sectionId(Property.ofValue("1"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getSubnets(), hasSize(1));
        assertThat(output.getSubnets().getFirst().getId(), is("10"));
        assertThat(output.getSubnets().getFirst().getSubnet(), is("10.0.0.0"));
    }

    @Test
    void subnetGet_happy_path() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/10/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody(
                    """
                    {"id":"10","subnet":"10.0.0.0","mask":"24","sectionId":"1","description":"Test"}
                    """))));

        var task = SubnetGet.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetGet.class.getName())
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
    void subnetCreate_returns_new_id() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api/myapp/subnets/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("\"42\""))));

        var task = SubnetCreate.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetCreate.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnet(Property.ofValue("192.168.1.0"))
            .mask(Property.ofValue("24"))
            .sectionId(Property.ofValue("1"))
            .resourceDescription(Property.ofValue("Office LAN"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("42"));
    }

    @Test
    void subnetSearch_by_cidr() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/cidr/192.168.1.0/24/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successListBody(
                    "[{\"id\":\"10\",\"subnet\":\"192.168.1.0\",\"mask\":\"24\",\"sectionId\":\"1\"}]"))));

        var task = SubnetSearch.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetSearch.class.getName())
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
    void subnetFirstFree_returns_cidr() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/10/first_subnet/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("\"192.168.1.128/25\""))));

        var task = SubnetFirstFree.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetFirstFree.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getCidr(), is("192.168.1.128/25"));
    }

    @Test
    void subnetDelete_sends_delete_request() throws Exception {
        wireMock.stubFor(delete(urlEqualTo("/api/myapp/subnets/10/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(WireMockSupport.successBody("null"))));

        var task = SubnetDelete.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetDelete.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("10"))
            .build();

        task.run(runContext());

        wireMock.verify(1, deleteRequestedFor(urlEqualTo("/api/myapp/subnets/10/")));
    }

    @Test
    void subnetGet_api_failure_throws_exception() {
        wireMock.stubFor(get(urlEqualTo("/api/myapp/subnets/999/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(WireMockSupport.failureBody(404, "Subnet not found"))));

        var task = SubnetGet.builder()
            .id(UUID.randomUUID().toString())
            .type(SubnetGet.class.getName())
            .baseUrl(Property.ofValue(WireMockSupport.baseUrl(wireMock)))
            .appId(Property.ofValue("myapp"))
            .auth(auth())
            .subnetId(Property.ofValue("999"))
            .build();

        var ex = Assertions.assertThrows(
            io.kestra.plugin.phpipam.PhpipamApiException.class,
            () -> task.run(runContext()));
        assertThat(ex.getMessage(), containsString("Subnet not found"));
    }
}
