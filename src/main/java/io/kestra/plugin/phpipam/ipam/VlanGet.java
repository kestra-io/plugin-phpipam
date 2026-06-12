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
import io.kestra.plugin.phpipam.ipam.model.Vlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Get a phpIPAM VLAN", description = "Retrieves a single VLAN by its numeric ID.")
@Plugin(
    examples = {
        @Example(
            title = "Get VLAN by ID",
            full = true,
            code = """
                id: phpipam_vlan_get
                namespace: company.team
                tasks:
                  - id: get_vlan
                    type: io.kestra.plugin.phpipam.ipam.VlanGet
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    vlanId: "5"
                """
        )
    }
)
public class VlanGet extends AbstractPhpipamTask implements RunnableTask<VlanGet.Output> {

    @Schema(title = "VLAN ID", description = "Numeric ID of the VLAN to retrieve.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vlanId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(vlanId).as(String.class).orElseThrow();
        var vlan = client.get("vlan/" + rId + "/", new TypeReference<PhpipamEnvelope<Vlan>>() {});
        return Output.builder().vlan(vlan).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "VLAN", description = "The retrieved VLAN.")
        private final Vlan vlan;
    }
}
