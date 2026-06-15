package io.kestra.plugin.phpipam.ipam.vlan;

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
import io.kestra.plugin.phpipam.ipam.model.Vlan;
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
@Schema(title = "List phpIPAM VLANs", description = "Returns all VLANs visible to the authenticated user.")
@Plugin(
    examples = {
        @Example(
            title = "List all VLANs",
            full = true,
            code = """
                id: phpipam_vlan_list
                namespace: company.team
                tasks:
                  - id: list_vlans
                    type: io.kestra.plugin.phpipam.ipam.vlan.List
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
            - `FETCH` (default): return the full list in `vlans` + total count.
            - `FETCH_ONE`: return only the first VLAN in `vlan` + total count.
            - `STORE`: write all VLANs as newline-delimited JSON to Kestra internal storage, return `uri` + total count.
            - `NONE`: return only the total count, no rows.
            """
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var vlans = client.get("vlan/", new TypeReference<PhpipamEnvelope<java.util.List<Vlan>>>() {});

            var rFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
            var safeVlans = vlans == null ? java.util.List.<Vlan>of() : vlans;
            int total = safeVlans.size();

            return switch (rFetchType) {
                case FETCH -> Output.builder().vlans(safeVlans).total(total).build();
                case FETCH_ONE -> Output.builder()
                    .vlan(safeVlans.isEmpty() ? null : safeVlans.getFirst())
                    .total(total)
                    .build();
                case STORE -> {
                    var ndjson = new StringBuilder();
                    for (var v : safeVlans) {
                        ndjson.append(MAPPER.writeValueAsString(v)).append("\n");
                    }
                    var bytes = ndjson.toString().getBytes(StandardCharsets.UTF_8);
                    var uri = runContext.storage().putFile(new ByteArrayInputStream(bytes), "vlans.ndjson");
                    yield Output.builder().uri(uri).total(total).build();
                }
                case NONE -> Output.builder().total(total).build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "VLANs", description = "All VLANs returned by the phpIPAM API. Populated for FETCH fetch type.")
        private final java.util.List<Vlan> vlans;

        @Schema(title = "VLAN", description = "First VLAN returned. Populated for FETCH_ONE fetch type.")
        private final Vlan vlan;

        @Schema(title = "URI", description = "Internal storage URI of the newline-delimited JSON file. Populated for STORE fetch type.")
        private final URI uri;

        @Schema(title = "Total", description = "Total number of VLANs returned by the API.")
        private final int total;
    }
}
