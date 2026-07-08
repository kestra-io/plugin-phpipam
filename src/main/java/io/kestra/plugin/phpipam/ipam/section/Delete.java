package io.kestra.plugin.phpipam.ipam.section;

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
    title = "Delete a phpIPAM section",
    description = "Permanently deletes a section by its numeric ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a section",
            full = true,
            code = """
                id: phpipam_section_delete
                namespace: company.team
                tasks:
                  - id: delete_section
                    type: io.kestra.plugin.phpipam.ipam.section.Delete
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    sectionId: "1"
                """
        )
    }
)
public class Delete extends AbstractPhpipamTask implements RunnableTask<Delete.Output> {

    @Schema(title = "Section ID", description = "Numeric ID of the section to delete.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sectionId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(sectionId).as(String.class).orElseThrow();
            client.delete("sections/" + rId + "/");
            return Output.builder().id(rId).deleted(true).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted section ID", description = "The numeric ID of the deleted section.")
        private final String id;

        @Schema(title = "Deleted", description = "True when the section was deleted.")
        private final Boolean deleted;
    }
}
