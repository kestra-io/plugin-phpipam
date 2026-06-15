package io.kestra.plugin.phpipam.ipam.section;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
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
@Schema(
    title = "Update a phpIPAM section",
    description = "Updates fields on an existing section. Only provided fields are modified."
)
@Plugin(
    examples = {
        @Example(
            title = "Update a section description",
            full = true,
            code = """
                id: phpipam_section_update
                namespace: company.team
                tasks:
                  - id: update_section
                    type: io.kestra.plugin.phpipam.ipam.section.Update
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    sectionId: "1"
                    resourceDescription: "Updated description"
                """
        )
    }
)
public class Update extends AbstractPhpipamTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Section ID", description = "Numeric ID of the section to update.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sectionId;

    @Schema(title = "Name", description = "New name for the section.")
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "Description", description = "New description for the section.")
    @PluginProperty(group = "main")
    private Property<String> resourceDescription;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(sectionId).as(String.class).orElseThrow();
            var body = new HashMap<String, Object>();
            body.put("id", rId);
            runContext.render(name).as(String.class).ifPresent(v -> body.put("name", v));
            runContext.render(resourceDescription).as(String.class).ifPresent(v -> body.put("description", v));

            client.patch("sections/" + rId + "/", body,
                new TypeReference<PhpipamEnvelope<Object>>() {});
            return new VoidOutput();
        }
    }
}
