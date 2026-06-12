package io.kestra.plugin.phpipam.ipam;

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
    title = "Create a phpIPAM address",
    description = "Registers a new IP address in the specified subnet."
)
@Plugin(
    examples = {
        @Example(
            title = "Create an address",
            full = true,
            code = """
                id: phpipam_address_create
                namespace: company.team
                tasks:
                  - id: create_address
                    type: io.kestra.plugin.phpipam.ipam.AddressCreate
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                    ip: "192.168.1.100"
                    hostname: "server01.example.com"
                    description: "Web server"
                """
        )
    }
)
public class AddressCreate extends AbstractPhpipamTask implements RunnableTask<AddressCreate.Output> {

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet the address belongs to.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Schema(title = "IP address", description = "The IPv4 or IPv6 address to register, e.g. `192.168.1.100`.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> ip;

    @Schema(title = "Hostname", description = "Optional hostname to associate with the address.")
    @PluginProperty(group = "main")
    private Property<String> hostname;

    @Schema(title = "Description", description = "Optional description of the address.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Schema(title = "Owner", description = "Optional owner name for this address.")
    @PluginProperty(group = "main")
    private Property<String> owner;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var body = new HashMap<String, Object>();
        body.put("subnetId", runContext.render(subnetId).as(String.class).orElseThrow());
        body.put("ip", runContext.render(ip).as(String.class).orElseThrow());
        runContext.render(hostname).as(String.class).ifPresent(v -> body.put("hostname", v));
        runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));
        runContext.render(owner).as(String.class).ifPresent(v -> body.put("owner", v));

        var id = client.postCreate("addresses/", body);
        return Output.builder().id(id).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created address ID", description = "Numeric ID of the newly created address record.")
        private final String id;
    }
}
