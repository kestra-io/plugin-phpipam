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
import io.kestra.plugin.phpipam.ipam.model.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List addresses in a phpIPAM subnet",
    description = "Returns all IP addresses belonging to the specified subnet."
)
@Plugin(
    examples = {
        @Example(
            title = "List addresses in a subnet",
            full = true,
            code = """
                id: phpipam_address_list
                namespace: company.team
                tasks:
                  - id: list_addresses
                    type: io.kestra.plugin.phpipam.ipam.AddressList
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                """
        )
    }
)
public class AddressList extends AbstractPhpipamTask implements RunnableTask<AddressList.Output> {

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet whose addresses to list.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(subnetId).as(String.class).orElseThrow();
        var addresses = client.get("subnets/" + rId + "/addresses/",
            new TypeReference<PhpipamEnvelope<List<Address>>>() {});
        return Output.builder().addresses(addresses).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Addresses", description = "IP addresses belonging to the subnet.")
        private final List<Address> addresses;
    }
}
