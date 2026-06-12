package io.kestra.plugin.phpipam.ipam.address;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update a phpIPAM address",
    description = "Updates fields on an existing IP address record. Only provided fields are modified."
)
@Plugin(
    examples = {
        @Example(
            title = "Update an address hostname",
            full = true,
            code = """
                id: phpipam_address_update
                namespace: company.team
                tasks:
                  - id: update_address
                    type: io.kestra.plugin.phpipam.ipam.address.Update
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    addressId: "42"
                    hostname: "new-hostname.example.com"
                """
        )
    }
)
public class Update extends AbstractPhpipamTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Address ID", description = "Numeric ID of the address record to update.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> addressId;

    @Schema(title = "Hostname", description = "New hostname.")
    @PluginProperty(group = "main")
    private Property<String> hostname;

    @Schema(title = "Description", description = "New description.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Schema(title = "Owner", description = "New owner.")
    @PluginProperty(group = "main")
    private Property<String> owner;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(addressId).as(String.class).orElseThrow();
        var body = new HashMap<String, Object>();
        body.put("id", rId);
        runContext.render(hostname).as(String.class).ifPresent(v -> body.put("hostname", v));
        runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));
        runContext.render(owner).as(String.class).ifPresent(v -> body.put("owner", v));

        client.patch("addresses/" + rId + "/", body,
            new TypeReference<PhpipamEnvelope<Object>>() {});
        return null;
    }
}
