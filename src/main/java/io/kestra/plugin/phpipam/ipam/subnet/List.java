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
    title = "List subnets in a phpIPAM section",
    description = "Returns all subnets belonging to the specified section."
)
@Plugin(
    examples = {
        @Example(
            title = "List subnets in a section",
            full = true,
            code = """
                id: phpipam_subnet_list
                namespace: company.team
                tasks:
                  - id: list_subnets
                    type: io.kestra.plugin.phpipam.ipam.subnet.List
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    sectionId: "1"
                """
        )
    }
)
public class List extends AbstractPhpipamTask implements RunnableTask<List.Output> {

    @Schema(title = "Section ID", description = "Numeric ID of the section whose subnets to list.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sectionId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(sectionId).as(String.class).orElseThrow();
        var subnets = client.get("sections/" + rId + "/subnets/",
            new TypeReference<PhpipamEnvelope<java.util.List<Subnet>>>() {});
        return Output.builder().subnets(subnets).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Subnets", description = "Subnets belonging to the section.")
        private final java.util.List<Subnet> subnets;
    }
}
