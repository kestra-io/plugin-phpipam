package io.kestra.plugin.phpipam.ipam;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.phpipam.PhpipamAuthentication;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end integration test that drives the real task classes against a live phpIPAM
 * instance (see {@code demo/docker-compose.yml}).
 * <p>
 * Disabled unless {@code PHPIPAM_IT_URL} is set, so it never runs in the normal unit-test build.
 * Bring phpIPAM up, then run:
 * <pre>
 * PHPIPAM_IT_URL=https://localhost:8443 PHPIPAM_IT_USER=admin PHPIPAM_IT_PASS=ipamadmin \
 *   ./gradlew test --tests '*PhpipamDockerIT'
 * </pre>
 * Each test carries a hard {@link Timeout} in a separate thread: if a task hangs (the bug behind
 * issues #14/#15), the test fails instead of blocking forever.
 */
@KestraTest
@EnabledIfEnvironmentVariable(named = "PHPIPAM_IT_URL", matches = ".+")
class PhpipamDockerIT {

    @Inject
    RunContextFactory runContextFactory;

    private static String env(String key, String def) {
        var v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }

    private static final String BASE = env("PHPIPAM_IT_URL", "https://localhost:8443");
    private static final String APP = env("PHPIPAM_IT_APP", "myapp");
    private static final String USER = env("PHPIPAM_IT_USER", "admin");
    private static final String PASS = env("PHPIPAM_IT_PASS", "ipamadmin");

    private PhpipamAuthentication auth() {
        return PhpipamAuthentication.builder()
            .username(Property.ofValue(USER))
            .password(Property.ofValue(PASS))
            .build();
    }

    private RunContext rc() {
        return runContextFactory.of();
    }

    /**
     * Issues #14 and #15: an Update (PATCH) followed by a Delete (DELETE) must each complete.
     * Before the fix, {@code PhpipamClient.close()} could block indefinitely after PATCH/DELETE.
     */
    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void update_then_delete_complete_without_hanging() throws Exception {
        var name = "it-crud-" + UUID.randomUUID();

        var created = io.kestra.plugin.phpipam.ipam.section.Create.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.Create.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .name(Property.ofValue(name))
            .build()
            .run(rc());
        assertThat(created.getId(), notNullValue());

        // Update — PATCH: must return (issue #14)
        io.kestra.plugin.phpipam.ipam.section.Update.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.Update.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .sectionId(Property.ofValue(created.getId()))
            .resourceDescription(Property.ofValue("updated by integration test"))
            .build()
            .run(rc());

        // Delete — DELETE: must return (issue #15)
        io.kestra.plugin.phpipam.ipam.section.Delete.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.Delete.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .sectionId(Property.ofValue(created.getId()))
            .build()
            .run(rc());
    }

    /**
     * Issue #16: {@code subnet.FirstFree} must include the mask segment
     * ({@code /subnets/{id}/first_subnet/{mask}/}) and return a CIDR instead of a 404.
     */
    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void subnet_first_free_with_mask_returns_cidr() throws Exception {
        var sectionId = io.kestra.plugin.phpipam.ipam.section.Create.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.Create.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .name(Property.ofValue("it-ff-" + UUID.randomUUID()))
            .build()
            .run(rc())
            .getId();

        var subnetId = io.kestra.plugin.phpipam.ipam.subnet.Create.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.subnet.Create.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .subnet(Property.ofValue("10.120.0.0"))
            .mask(Property.ofValue("24"))
            .sectionId(Property.ofValue(sectionId))
            .build()
            .run(rc())
            .getId();

        var firstFree = io.kestra.plugin.phpipam.ipam.subnet.FirstFree.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.subnet.FirstFree.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .subnetId(Property.ofValue(subnetId))
            .mask(Property.ofValue("25"))
            .build()
            .run(rc());

        assertThat(firstFree.getCidr(), notNullValue());
        assertThat(firstFree.getCidr(), containsString("/25"));

        // cleanup
        io.kestra.plugin.phpipam.ipam.subnet.Delete.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.subnet.Delete.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .subnetId(Property.ofValue(subnetId))
            .build()
            .run(rc());

        io.kestra.plugin.phpipam.ipam.section.Delete.builder()
            .id(UUID.randomUUID().toString())
            .type(io.kestra.plugin.phpipam.ipam.section.Delete.class.getName())
            .baseUrl(Property.ofValue(BASE))
            .appId(Property.ofValue(APP))
            .auth(auth())
            .insecureTls(Property.ofValue(true))
            .sectionId(Property.ofValue(sectionId))
            .build()
            .run(rc());
    }
}
