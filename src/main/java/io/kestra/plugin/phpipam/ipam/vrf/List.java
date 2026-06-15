package io.kestra.plugin.phpipam.ipam.vrf;

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
import io.kestra.plugin.phpipam.ipam.model.Vrf;
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
@Schema(title = "List phpIPAM VRFs", description = "Returns all VRFs visible to the authenticated user.")
@Plugin(
    examples = {
        @Example(
            title = "List all VRFs",
            full = true,
            code = """
                id: phpipam_vrf_list
                namespace: company.team
                tasks:
                  - id: list_vrfs
                    type: io.kestra.plugin.phpipam.ipam.vrf.List
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
            - `FETCH` (default): return the full list in `vrfs` + total count.
            - `FETCH_ONE`: return only the first VRF in `vrf` + total count.
            - `STORE`: write all VRFs as newline-delimited JSON to Kestra internal storage, return `uri` + total count.
            - `NONE`: return only the total count, no rows.
            """
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var vrfs = client.get("vrf/", new TypeReference<PhpipamEnvelope<java.util.List<Vrf>>>() {});

            var rFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
            var safeVrfs = vrfs == null ? java.util.List.<Vrf>of() : vrfs;
            int total = safeVrfs.size();

            return switch (rFetchType) {
                case FETCH -> Output.builder().vrfs(safeVrfs).total(total).build();
                case FETCH_ONE -> Output.builder()
                    .vrf(safeVrfs.isEmpty() ? null : safeVrfs.getFirst())
                    .total(total)
                    .build();
                case STORE -> {
                    var ndjson = new StringBuilder();
                    for (var v : safeVrfs) {
                        ndjson.append(MAPPER.writeValueAsString(v)).append("\n");
                    }
                    var bytes = ndjson.toString().getBytes(StandardCharsets.UTF_8);
                    var uri = runContext.storage().putFile(new ByteArrayInputStream(bytes), "vrfs.ndjson");
                    yield Output.builder().uri(uri).total(total).build();
                }
                case NONE -> Output.builder().total(total).build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "VRFs", description = "All VRFs returned by the phpIPAM API. Populated for FETCH fetch type.")
        private final java.util.List<Vrf> vrfs;

        @Schema(title = "VRF", description = "First VRF returned. Populated for FETCH_ONE fetch type.")
        private final Vrf vrf;

        @Schema(title = "URI", description = "Internal storage URI of the newline-delimited JSON file. Populated for STORE fetch type.")
        private final URI uri;

        @Schema(title = "Total", description = "Total number of VRFs returned by the API.")
        private final int total;
    }
}
