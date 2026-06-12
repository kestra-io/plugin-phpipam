package io.kestra.plugin.phpipam.ipam.subnet;

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
@Schema(
    title = "Create a phpIPAM subnet",
    description = "Creates a new subnet in the specified section."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a subnet",
            full = true,
            code = """
                id: phpipam_subnet_create
                namespace: company.team
                tasks:
                  - id: create_subnet
                    type: io.kestra.plugin.phpipam.ipam.subnet.Create
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnet: "192.168.1.0"
                    mask: "24"
                    sectionId: "1"
                    description: "Office LAN"
                """
        )
    }
)
public class Create extends AbstractPhpipamTask implements RunnableTask<Create.Output> {

    @Schema(title = "Subnet address", description = "Network address of the subnet, e.g. `192.168.1.0`.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnet;

    @Schema(title = "Mask", description = "CIDR prefix length, e.g. `24`.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> mask;

    @Schema(title = "Section ID", description = "Numeric ID of the section this subnet belongs to.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sectionId;

    @Schema(title = "Description", description = "Optional description of the subnet.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Schema(title = "VLAN ID", description = "Optional numeric VLAN ID to associate.")
    @PluginProperty(group = "main")
    private Property<String> vlanId;

    @Schema(title = "VRF ID", description = "Optional numeric VRF ID to associate.")
    @PluginProperty(group = "main")
    private Property<String> vrfId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var body = new HashMap<String, Object>();
        body.put("subnet", runContext.render(subnet).as(String.class).orElseThrow());
        body.put("mask", runContext.render(mask).as(String.class).orElseThrow());
        body.put("sectionId", runContext.render(sectionId).as(String.class).orElseThrow());
        runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));
        runContext.render(vlanId).as(String.class).ifPresent(v -> body.put("vlanId", v));
        runContext.render(vrfId).as(String.class).ifPresent(v -> body.put("vrfId", v));

        var id = client.postCreate("subnets/", body);
        return Output.builder().id(id).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created subnet ID", description = "Numeric ID of the newly created subnet.")
        private final String id;
    }
}
