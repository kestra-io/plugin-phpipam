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
    title = "Get the first free IP address in a subnet",
    description = """
        Returns the first unallocated IP address within the given subnet.
        Raises an error if the subnet is full.
        Combine with `AddressCreate` to atomically reserve the next available address.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Get first free address",
            full = true,
            code = """
                id: phpipam_address_first_free
                namespace: company.team
                tasks:
                  - id: first_free
                    type: io.kestra.plugin.phpipam.ipam.AddressFirstFree
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                """
        )
    }
)
public class AddressFirstFree extends AbstractPhpipamTask implements RunnableTask<AddressFirstFree.Output> {

    @Schema(
        title = "Subnet ID",
        description = "Numeric ID of the subnet from which the first free IP address is requested."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(subnetId).as(String.class).orElseThrow();
        var ip = client.get("subnets/" + rId + "/first_free/",
            new TypeReference<PhpipamEnvelope<String>>() {});
        return Output.builder().ip(ip).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "First free IP", description = "The first unallocated IP address in the subnet.")
        private final String ip;
    }
}
