package io.kestra.plugin.phpipam.ipam.section;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Section;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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

    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @Schema(
        title = "Fetch type",
        description = """
            Controls how results are returned:
            - `FETCH` (default): return the full list in `sections` + total count.
            - `FETCH_ONE`: return only the first section in `section` + total count.
            - `STORE`: write all sections as newline-delimited JSON to Kestra internal storage, return `uri` + total count.
            - `NONE`: return only the total count, no rows.
            """
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var sections = client.get("sections/",
                new TypeReference<PhpipamEnvelope<java.util.List<Section>>>() {});

            var rFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
            var safeSections = sections == null ? java.util.List.<Section>of() : sections;
            int total = safeSections.size();

            return switch (rFetchType) {
                case FETCH -> Output.builder().sections(safeSections).total(total).build();
                case FETCH_ONE -> Output.builder()
                    .section(safeSections.isEmpty() ? null : safeSections.getFirst())
                    .total(total)
                    .build();
                case STORE -> {
                    var ndjson = new StringBuilder();
                    for (var sec : safeSections) {
                        ndjson.append(MAPPER.writeValueAsString(sec)).append("\n");
                    }
                    var bytes = ndjson.toString().getBytes(StandardCharsets.UTF_8);
                    var uri = runContext.storage().putFile(new ByteArrayInputStream(bytes), "sections.ndjson");
                    yield Output.builder().uri(uri).total(total).build();
                }
                case NONE -> Output.builder().total(total).build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "List of sections", description = "All sections returned by the phpIPAM API. Populated for FETCH fetch type.")
        private final java.util.List<Section> sections;

        @Schema(title = "Section", description = "First section returned. Populated for FETCH_ONE fetch type.")
        private final Section section;

        @Schema(title = "URI", description = "Internal storage URI of the newline-delimited JSON file. Populated for STORE fetch type.")
        private final URI uri;

        @Schema(title = "Total", description = "Total number of sections returned by the API.")
        private final int total;
    }
}
