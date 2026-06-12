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
import io.kestra.plugin.phpipam.ipam.model.Vrf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Get a phpIPAM VRF", description = "Retrieves a single VRF by its numeric ID.")
@Plugin(
    examples = {
        @Example(
            title = "Get VRF by ID",
            full = true,
            code = """
                id: phpipam_vrf_get
                namespace: company.team
                tasks:
                  - id: get_vrf
                    type: io.kestra.plugin.phpipam.ipam.VrfGet
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    vrfId: "2"
                """
        )
    }
)
public class VrfGet extends AbstractPhpipamTask implements RunnableTask<VrfGet.Output> {

    @Schema(title = "VRF ID", description = "Numeric ID of the VRF to retrieve.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vrfId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(vrfId).as(String.class).orElseThrow();
        var vrf = client.get("vrf/" + rId + "/", new TypeReference<PhpipamEnvelope<Vrf>>() {});
        return Output.builder().vrf(vrf).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "VRF", description = "The retrieved VRF.")
        private final Vrf vrf;
    }
}
