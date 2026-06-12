package io.kestra.plugin.phpipam.ipam.vrf;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Vrf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var vrfs = client.get("vrf/", new TypeReference<PhpipamEnvelope<java.util.List<Vrf>>>() {});
        return Output.builder().vrfs(vrfs).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "VRFs", description = "All VRFs returned by the phpIPAM API.")
        private final java.util.List<Vrf> vrfs;
    }
}
