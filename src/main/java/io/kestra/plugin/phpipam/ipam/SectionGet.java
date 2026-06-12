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
import io.kestra.plugin.phpipam.ipam.model.Section;
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
    title = "Get a phpIPAM section",
    description = "Retrieves a single section by its numeric ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get section by ID",
            full = true,
            code = """
                id: phpipam_section_get
                namespace: company.team
                tasks:
                  - id: get_section
                    type: io.kestra.plugin.phpipam.ipam.SectionGet
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    sectionId: "1"
                """
        )
    }
)
public class SectionGet extends AbstractPhpipamTask implements RunnableTask<SectionGet.Output> {

    @Schema(title = "Section ID", description = "Numeric ID of the section to retrieve.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sectionId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var rId = runContext.render(sectionId).as(String.class).orElseThrow();
        var section = client.get("sections/" + rId + "/",
            new TypeReference<PhpipamEnvelope<Section>>() {});
        return Output.builder().section(section).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Section", description = "The retrieved section.")
        private final Section section;
    }
}
