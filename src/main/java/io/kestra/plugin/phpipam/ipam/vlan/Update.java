package io.kestra.plugin.phpipam.ipam.vlan;

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
@Schema(title = "Update a phpIPAM VLAN", description = "Updates fields on an existing VLAN.")
@Plugin(
    examples = {
        @Example(
            title = "Update a VLAN description",
            full = true,
            code = """
                id: phpipam_vlan_update
                namespace: company.team
                tasks:
                  - id: update_vlan
                    type: io.kestra.plugin.phpipam.ipam.vlan.Update
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    vlanId: "5"
                    description: "Updated VLAN description"
                """
        )
    }
)
public class Update extends AbstractPhpipamTask implements RunnableTask<VoidOutput> {

    @Schema(title = "VLAN ID", description = "Numeric ID of the VLAN to update.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vlanId;

    @Schema(title = "Name", description = "New name for the VLAN.")
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "Description", description = "New description for the VLAN.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(vlanId).as(String.class).orElseThrow();
        var body = new HashMap<String, Object>();
        body.put("vlanId", rId);
        runContext.render(name).as(String.class).ifPresent(v -> body.put("name", v));
        runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));

        client.patch("vlan/" + rId + "/", body, new TypeReference<PhpipamEnvelope<Object>>() {});
        return null;
    }
}
