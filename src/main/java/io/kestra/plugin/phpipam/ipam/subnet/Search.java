package io.kestra.plugin.phpipam.ipam.subnet;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Subnet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.regex.Pattern;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Search phpIPAM subnets by CIDR",
    description = """
        Searches for subnets matching a given CIDR notation (e.g. `192.168.1.0/24`).
        Returns all subnets that match, across all sections visible to the authenticated user.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Search subnets by CIDR",
            full = true,
            code = """
                id: phpipam_subnet_search
                namespace: company.team
                tasks:
                  - id: search_subnet
                    type: io.kestra.plugin.phpipam.ipam.subnet.Search
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    cidr: "192.168.1.0/24"
                """
        )
    }
)
public class Search extends AbstractPhpipamTask implements RunnableTask<Search.Output> {

    private static final Pattern CIDR_PATTERN =
        Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}/\\d{1,2}$");

    @Schema(
        title = "CIDR to search",
        description = "Subnet in CIDR notation, e.g. `192.168.1.0/24`. The API returns subnets whose address and mask match."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> cidr;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rCidr = runContext.render(cidr).as(String.class).orElseThrow();

        if (!CIDR_PATTERN.matcher(rCidr).matches()) {
            throw new IllegalArgumentException("Invalid CIDR format: '" + rCidr
                + "'. Expected IPv4 CIDR notation, e.g. 192.168.1.0/24");
        }

        var subnets = client.get("subnets/cidr/" + rCidr + "/",
            new TypeReference<PhpipamEnvelope<java.util.List<Subnet>>>() {});
        return Output.builder().subnets(subnets).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Matching subnets", description = "Subnets whose address and mask match the given CIDR.")
        private final java.util.List<Subnet> subnets;
    }
}
