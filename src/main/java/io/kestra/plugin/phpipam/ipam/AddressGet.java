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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a phpIPAM address",
    description = "Retrieves a single IP address record by its numeric ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get address by ID",
            full = true,
            code = """
                id: phpipam_address_get
                namespace: company.team
                tasks:
                  - id: get_address
                    type: io.kestra.plugin.phpipam.ipam.AddressGet
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    addressId: "42"
                """
        )
    }
)
public class AddressGet extends AbstractPhpipamTask implements RunnableTask<AddressGet.Output> {

    @Schema(title = "Address ID", description = "Numeric ID of the address record to retrieve.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> addressId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(addressId).as(String.class).orElseThrow();
        var address = client.get("addresses/" + rId + "/",
            new TypeReference<PhpipamEnvelope<Address>>() {});
        return Output.builder().address(address).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Address", description = "The retrieved IP address record.")
        private final Address address;
    }
}
