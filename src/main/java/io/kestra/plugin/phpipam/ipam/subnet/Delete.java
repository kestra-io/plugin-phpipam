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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a phpIPAM subnet",
    description = "Permanently deletes a subnet by its numeric ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a subnet",
            full = true,
            code = """
                id: phpipam_subnet_delete
                namespace: company.team
                tasks:
                  - id: delete_subnet
                    type: io.kestra.plugin.phpipam.ipam.subnet.Delete
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                """
        )
    }
)
public class Delete extends AbstractPhpipamTask implements RunnableTask<Delete.Output> {

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet to delete.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(subnetId).as(String.class).orElseThrow();
            client.delete("subnets/" + rId + "/");
            return Output.builder().id(rId).deleted(true).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted subnet ID", description = "The numeric ID of the deleted subnet.")
        private final String id;

        @Schema(title = "Deleted", description = "True when the subnet was deleted.")
        private final Boolean deleted;
    }
}
