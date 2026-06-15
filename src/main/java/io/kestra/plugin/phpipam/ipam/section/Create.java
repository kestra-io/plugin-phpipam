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

import java.util.HashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a phpIPAM section",
    description = "Creates a new section in phpIPAM."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a section",
            full = true,
            code = """
                id: phpipam_section_create
                namespace: company.team
                tasks:
                  - id: create_section
                    type: io.kestra.plugin.phpipam.ipam.section.Create
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    name: "Production"
                    description: "Production network section"
                """
        )
    }
)
public class Create extends AbstractPhpipamTask implements RunnableTask<Create.Output> {

    @Schema(title = "Section name", description = "Unique name for the new section.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "Description", description = "Optional description of the section.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Schema(title = "Master section ID", description = "ID of the parent section, if this is a sub-section.")
    @PluginProperty(group = "main")
    private Property<String> masterSection;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var body = new HashMap<String, Object>();
            body.put("name", runContext.render(name).as(String.class).orElseThrow());
            runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));
            runContext.render(masterSection).as(String.class).ifPresent(v -> body.put("masterSection", v));

            var id = client.postCreate("sections/", body);
            return Output.builder().id(id).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created section ID", description = "The numeric ID of the newly created section.")
        private final String id;
    }
}
