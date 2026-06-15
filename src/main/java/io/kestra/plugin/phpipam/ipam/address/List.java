package io.kestra.plugin.phpipam.ipam.address;

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
import io.kestra.plugin.phpipam.ipam.model.Address;
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
    title = "List addresses in a phpIPAM subnet",
    description = "Returns IP addresses belonging to the specified subnet."
)
@Plugin(
    examples = {
        @Example(
            title = "List addresses in a subnet",
            full = true,
            code = """
                id: phpipam_address_list
                namespace: company.team
                tasks:
                  - id: list_addresses
                    type: io.kestra.plugin.phpipam.ipam.address.List
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                """
        )
    }
)
public class List extends AbstractPhpipamTask implements RunnableTask<List.Output> {

    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet whose addresses to list.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Schema(
        title = "Fetch type",
        description = """
            Controls how results are returned:
            - `FETCH` (default): return the full list in `addresses` + total count.
            - `FETCH_ONE`: return only the first address in `address` + total count.
            - `STORE`: write all addresses as newline-delimited JSON to Kestra internal storage, return `uri` + total count.
            - `NONE`: return only the total count, no rows.
            """
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (var client = buildClient(runContext)) {
            var rId = runContext.render(subnetId).as(String.class).orElseThrow();
            var addresses = client.get("subnets/" + rId + "/addresses/",
                new TypeReference<PhpipamEnvelope<java.util.List<Address>>>() {});

            var rFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
            var safeAddresses = addresses == null ? java.util.List.<Address>of() : addresses;
            int total = safeAddresses.size();

            return switch (rFetchType) {
                case FETCH -> Output.builder().addresses(safeAddresses).total(total).build();
                case FETCH_ONE -> Output.builder()
                    .address(safeAddresses.isEmpty() ? null : safeAddresses.getFirst())
                    .total(total)
                    .build();
                case STORE -> {
                    var ndjson = new StringBuilder();
                    for (var addr : safeAddresses) {
                        ndjson.append(MAPPER.writeValueAsString(addr)).append("\n");
                    }
                    var bytes = ndjson.toString().getBytes(StandardCharsets.UTF_8);
                    var uri = runContext.storage().putFile(new ByteArrayInputStream(bytes), "addresses.ndjson");
                    yield Output.builder().uri(uri).total(total).build();
                }
                case NONE -> Output.builder().total(total).build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Addresses", description = "IP addresses belonging to the subnet. Populated for FETCH fetch type.")
        private final java.util.List<Address> addresses;

        @Schema(title = "Address", description = "First address in the subnet. Populated for FETCH_ONE fetch type.")
        private final Address address;

        @Schema(title = "URI", description = "Internal storage URI of the newline-delimited JSON file. Populated for STORE fetch type.")
        private final URI uri;

        @Schema(title = "Total", description = "Total number of addresses returned by the API.")
        private final int total;
    }
}
