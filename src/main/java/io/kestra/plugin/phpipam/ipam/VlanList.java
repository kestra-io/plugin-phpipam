package io.kestra.plugin.phpipam.ipam;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Vlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
                    type: io.kestra.plugin.phpipam.ipam.VlanList
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                """
        )
    }
)
public class VlanList extends AbstractPhpipamTask implements RunnableTask<VlanList.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var client = buildClient(runContext);
        var vlans = client.get("vlan/", new TypeReference<PhpipamEnvelope<List<Vlan>>>() {});
        return Output.builder().vlans(vlans).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "VLANs", description = "All VLANs returned by the phpIPAM API.")
        private final List<Vlan> vlans;
    }
}
