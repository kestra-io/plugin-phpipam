package io.kestra.plugin.phpipam.ipam.vrf;

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
@Schema(title = "Delete a phpIPAM VRF", description = "Permanently deletes a VRF by its numeric ID.")
@Plugin(
    examples = {
        @Example(
            title = "Delete a VRF",
            full = true,
            code = """
                id: phpipam_vrf_delete
                namespace: company.team
                tasks:
                  - id: delete_vrf
                    type: io.kestra.plugin.phpipam.ipam.vrf.Delete
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    vrfId: "2"
                """
        )
    }
)
public class Delete extends AbstractPhpipamTask implements RunnableTask<VoidOutput> {

    @Schema(title = "VRF ID", description = "Numeric ID of the VRF to delete.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vrfId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(vrfId).as(String.class).orElseThrow();
        client.delete("vrf/" + rId + "/");
        return null;
    }
}
