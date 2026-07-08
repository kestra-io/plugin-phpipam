package io.kestra.plugin.phpipam.ipam.vrf;

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

import java.util.HashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Update a phpIPAM VRF", description = "Updates fields on an existing VRF.")
@Plugin(
    examples = {
        @Example(
            title = "Update a VRF description",
            full = true,
            code = """
                id: phpipam_vrf_update
                namespace: company.team
                tasks:
                  - id: update_vrf
                    type: io.kestra.plugin.phpipam.ipam.vrf.Update
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    vrfId: "2"
                    resourceDescription: "Updated VRF description"
                """
        )
    }
)
public class Update extends AbstractPhpipamTask implements RunnableTask<Update.Output> {

    @Schema(title = "VRF ID", description = "Numeric ID of the VRF to update.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vrfId;

    @Schema(title = "Name", description = "New name for the VRF.")
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "Route distinguisher", description = "New Route Distinguisher.")
    @PluginProperty(group = "main")
    private Property<String> rd;

    @Schema(title = "Description", description = "New description.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(vrfId).as(String.class).orElseThrow();
            var body = new HashMap<String, Object>();
            body.put("vrfId", rId);
            runContext.render(name).as(String.class).ifPresent(v -> body.put("name", v));
            runContext.render(rd).as(String.class).ifPresent(v -> body.put("rd", v));
            runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));

            client.patch("vrf/" + rId + "/", body, new TypeReference<PhpipamEnvelope<Object>>() {});
            return Output.builder().id(rId).updated(true).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Updated VRF ID", description = "The numeric ID of the updated VRF.")
        private final String id;

        @Schema(title = "Updated", description = "True when the VRF was updated.")
        private final Boolean updated;
    }
}
