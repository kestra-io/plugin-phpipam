package io.kestra.plugin.phpipam.ipam.subnet;

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
import io.kestra.plugin.phpipam.ipam.model.Subnet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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

    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @Schema(title = "Section ID", description = "Numeric ID of the section whose subnets to list.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sectionId;

    @Schema(
        title = "Fetch type",
        description = """
            Controls how results are returned:
            - `FETCH` (default): return the full list in `subnets` + total count.
            - `FETCH_ONE`: return only the first subnet in `subnet` + total count.
            - `STORE`: write all subnets as newline-delimited JSON to Kestra internal storage, return `uri` + total count.
            - `NONE`: return only the total count, no rows.
            """
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(sectionId).as(String.class).orElseThrow();
            var subnets = client.get("sections/" + rId + "/subnets/",
                new TypeReference<PhpipamEnvelope<java.util.List<Subnet>>>() {});

            var rFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
            var safeSubnets = subnets == null ? java.util.List.<Subnet>of() : subnets;
            int total = safeSubnets.size();

            return switch (rFetchType) {
                case FETCH -> Output.builder().subnets(safeSubnets).total(total).build();
                case FETCH_ONE -> Output.builder()
                    .subnet(safeSubnets.isEmpty() ? null : safeSubnets.getFirst())
                    .total(total)
                    .build();
                case STORE -> {
                    var ndjson = new StringBuilder();
                    for (var sub : safeSubnets) {
                        ndjson.append(MAPPER.writeValueAsString(sub)).append("\n");
                    }
                    var bytes = ndjson.toString().getBytes(StandardCharsets.UTF_8);
                    var uri = runContext.storage().putFile(new ByteArrayInputStream(bytes), "subnets.ndjson");
                    yield Output.builder().uri(uri).total(total).build();
                }
                case NONE -> Output.builder().total(total).build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Subnets", description = "Subnets belonging to the section. Populated for FETCH fetch type.")
        private final java.util.List<Subnet> subnets;

        @Schema(title = "Subnet", description = "First subnet returned. Populated for FETCH_ONE fetch type.")
        private final Subnet subnet;

        @Schema(title = "URI", description = "Internal storage URI of the newline-delimited JSON file. Populated for STORE fetch type.")
        private final URI uri;

        @Schema(title = "Total", description = "Total number of subnets returned by the API.")
        private final int total;
    }
}
