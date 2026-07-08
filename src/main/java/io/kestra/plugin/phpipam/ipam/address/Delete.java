package io.kestra.plugin.phpipam.ipam.address;

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
    title = "Delete a phpIPAM address",
    description = "Permanently deletes an IP address record by its numeric ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete an address",
            full = true,
            code = """
                id: phpipam_address_delete
                namespace: company.team
                tasks:
                  - id: delete_address
                    type: io.kestra.plugin.phpipam.ipam.address.Delete
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    addressId: "42"
                """
        )
    }
)
public class Delete extends AbstractPhpipamTask implements RunnableTask<Delete.Output> {

    @Schema(title = "Address ID", description = "Numeric ID of the address record to delete.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> addressId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(addressId).as(String.class).orElseThrow();
            client.delete("addresses/" + rId + "/");
            return Output.builder().id(rId).deleted(true).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted address ID", description = "The numeric ID of the deleted address record.")
        private final String id;

        @Schema(title = "Deleted", description = "True when the address record was deleted.")
        private final Boolean deleted;
    }
}
