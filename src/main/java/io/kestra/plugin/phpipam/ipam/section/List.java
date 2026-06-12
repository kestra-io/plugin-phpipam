package io.kestra.plugin.phpipam.ipam.section;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Section;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List phpIPAM sections",
    description = "Returns all sections visible to the authenticated user."
)
@Plugin(
    examples = {
        @Example(
            title = "List all sections",
            full = true,
            code = """
                id: phpipam_section_list
                namespace: company.team
                tasks:
                  - id: list_sections
                    type: io.kestra.plugin.phpipam.ipam.section.List
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                """
        )
    }
)
public class List extends AbstractPhpipamTask implements RunnableTask<List.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var sections = client.get("sections/",
            new TypeReference<PhpipamEnvelope<java.util.List<Section>>>() {});
        return Output.builder().sections(sections).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "List of sections", description = "All sections returned by the phpIPAM API.")
        private final java.util.List<Section> sections;
    }
}
