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
@Schema(title = "Create a phpIPAM VLAN", description = "Creates a new VLAN in phpIPAM.")
@Plugin(
    examples = {
        @Example(
            title = "Create a VLAN",
            full = true,
            code = """
                id: phpipam_vlan_create
                namespace: company.team
                tasks:
                  - id: create_vlan
                    type: io.kestra.plugin.phpipam.ipam.VlanCreate
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    name: "MGMT"
                    number: "100"
                    description: "Management VLAN"
                """
        )
    }
)
public class VlanCreate extends AbstractPhpipamTask implements RunnableTask<VlanCreate.Output> {

    @Schema(title = "VLAN name", description = "Name for the new VLAN.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "VLAN number", description = "802.1Q VLAN tag number (1–4094).")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> number;

    @Schema(title = "Description", description = "Optional description.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var body = new HashMap<String, Object>();
        body.put("name", runContext.render(name).as(String.class).orElseThrow());
        body.put("number", runContext.render(number).as(String.class).orElseThrow());
        runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));

        var id = client.post("vlan/", body, new TypeReference<PhpipamEnvelope<String>>() {});
        return Output.builder().id(id).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created VLAN ID", description = "Numeric ID of the newly created VLAN.")
        private final String id;
    }
}
