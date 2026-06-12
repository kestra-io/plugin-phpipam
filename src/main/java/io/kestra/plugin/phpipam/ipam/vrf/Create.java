package io.kestra.plugin.phpipam.ipam.vrf;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
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
@Schema(title = "Create a phpIPAM VRF", description = "Creates a new VRF in phpIPAM.")
@Plugin(
    examples = {
        @Example(
            title = "Create a VRF",
            full = true,
            code = """
                id: phpipam_vrf_create
                namespace: company.team
                tasks:
                  - id: create_vrf
                    type: io.kestra.plugin.phpipam.ipam.vrf.Create
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    name: "CORP-VRF"
                    rd: "65000:1"
                    description: "Corporate routing domain"
                """
        )
    }
)
public class Create extends AbstractPhpipamTask implements RunnableTask<Create.Output> {

    @Schema(title = "VRF name", description = "Name for the new VRF.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "Route distinguisher", description = "BGP Route Distinguisher in `ASN:nn` format, e.g. `65000:1`.")
    @PluginProperty(group = "main")
    private Property<String> rd;

    @Schema(title = "Description", description = "Optional description of the VRF.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var body = new HashMap<String, Object>();
        body.put("name", runContext.render(name).as(String.class).orElseThrow());
        runContext.render(rd).as(String.class).ifPresent(v -> body.put("rd", v));
        runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));

        var id = client.postCreate("vrf/", body);
        return Output.builder().id(id).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created VRF ID", description = "Numeric ID of the newly created VRF.")
        private final String id;
    }
}
