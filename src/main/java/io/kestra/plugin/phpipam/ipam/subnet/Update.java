package io.kestra.plugin.phpipam.ipam.subnet;

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
    title = "Update a phpIPAM subnet",
    description = "Updates fields on an existing subnet. Only provided fields are modified."
)
@Plugin(
    examples = {
        @Example(
            title = "Update a subnet description",
            full = true,
            code = """
                id: phpipam_subnet_update
                namespace: company.team
                tasks:
                  - id: update_subnet
                    type: io.kestra.plugin.phpipam.ipam.subnet.Update
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                    resourceDescription: "Updated description"
                """
        )
    }
)
public class Update extends AbstractPhpipamTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet to update.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Schema(title = "Description", description = "New description for the subnet.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Schema(title = "VLAN ID", description = "VLAN ID to associate with the subnet.")
    @PluginProperty(group = "main")
    private Property<String> vlanId;

    @Schema(title = "VRF ID", description = "VRF ID to associate with the subnet.")
    @PluginProperty(group = "main")
    private Property<String> vrfId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(subnetId).as(String.class).orElseThrow();
            var body = new HashMap<String, Object>();
            body.put("id", rId);
            runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));
            runContext.render(vlanId).as(String.class).ifPresent(v -> body.put("vlanId", v));
            runContext.render(vrfId).as(String.class).ifPresent(v -> body.put("vrfId", v));

            client.patch("subnets/" + rId + "/", body,
                new TypeReference<PhpipamEnvelope<Object>>() {});
            return new VoidOutput();
        }
    }
}
