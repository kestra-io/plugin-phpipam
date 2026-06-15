package io.kestra.plugin.phpipam.ipam.subnet;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Subnet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a phpIPAM subnet",
    description = "Retrieves a single subnet by its numeric ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get subnet by ID",
            full = true,
            code = """
                id: phpipam_subnet_get
                namespace: company.team
                tasks:
                  - id: get_subnet
                    type: io.kestra.plugin.phpipam.ipam.subnet.Get
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                """
        )
    }
)
public class Get extends AbstractPhpipamTask implements RunnableTask<Get.Output> {

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet to retrieve.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(subnetId).as(String.class).orElseThrow();
            var subnet = client.get("subnets/" + rId + "/",
                new TypeReference<PhpipamEnvelope<Subnet>>() {});
            return Output.builder().subnet(subnet).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Subnet", description = "The retrieved subnet.")
        private final Subnet subnet;
    }
}
