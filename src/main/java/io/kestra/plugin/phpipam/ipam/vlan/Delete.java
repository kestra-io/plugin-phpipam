package io.kestra.plugin.phpipam.ipam.vlan;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Delete a phpIPAM VLAN", description = "Permanently deletes a VLAN by its numeric ID.")
@Plugin(
    examples = {
        @Example(
            title = "Delete a VLAN",
            full = true,
            code = """
                id: phpipam_vlan_delete
                namespace: company.team
                tasks:
                  - id: delete_vlan
                    type: io.kestra.plugin.phpipam.ipam.vlan.Delete
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    vlanId: "5"
                """
        )
    }
)
public class Delete extends AbstractPhpipamTask implements RunnableTask<VoidOutput> {

    @Schema(title = "VLAN ID", description = "Numeric ID of the VLAN to delete.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vlanId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(vlanId).as(String.class).orElseThrow();
            client.delete("vlan/" + rId + "/");
            return new VoidOutput();
        }
    }
}
