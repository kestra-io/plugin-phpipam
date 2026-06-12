package io.kestra.plugin.phpipam.ipam;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get the first free subnet within a master subnet",
    description = """
        Returns the CIDR of the first available child-subnet slot within the given master subnet.
        Useful for automatically provisioning the next available prefix from a supernet.
        Raises an error if no free subnet slot exists.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Get first free subnet",
            full = true,
            code = """
                id: phpipam_subnet_first_free
                namespace: company.team
                tasks:
                  - id: first_free_subnet
                    type: io.kestra.plugin.phpipam.ipam.SubnetFirstFree
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "5"
                """
        )
    }
)
public class SubnetFirstFree extends AbstractPhpipamTask implements RunnableTask<SubnetFirstFree.Output> {

    @Schema(
        title = "Master subnet ID",
        description = "Numeric ID of the master subnet from which the first free child subnet is requested."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(subnetId).as(String.class).orElseThrow();
        var cidr = client.get("subnets/" + rId + "/first_subnet/",
            new TypeReference<PhpipamEnvelope<String>>() {});
        return Output.builder().cidr(cidr).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "First free subnet CIDR", description = "CIDR notation of the first available child subnet slot.")
        private final String cidr;
    }
}
