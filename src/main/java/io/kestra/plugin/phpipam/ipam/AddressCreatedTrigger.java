package io.kestra.plugin.phpipam.ipam;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.*;
import io.kestra.core.storages.kv.KVMetadata;
import io.kestra.core.storages.kv.KVValueAndMetadata;
import io.kestra.plugin.phpipam.AbstractPhpipamTask;
import io.kestra.plugin.phpipam.PhpipamEnvelope;
import io.kestra.plugin.phpipam.ipam.model.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger on new IP addresses assigned in a phpIPAM subnet",
    description = """
        Polls a phpIPAM subnet at a configurable interval and fires a new execution
        for every IP address that was not seen during the previous poll.
        Deduplication is achieved via the address `id` field stored in the namespace KV store.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Trigger when a new address is assigned",
            full = true,
            code = """
                id: phpipam_new_address_trigger
                namespace: company.team
                triggers:
                  - id: watch_subnet
                    type: io.kestra.plugin.phpipam.ipam.AddressCreatedTrigger
                    baseUrl: "https://ipam.example.com"
                    appId: myapp
                    auth:
                      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
                    subnetId: "10"
                    interval: PT5M
                tasks:
                  - id: log_new_address
                    type: io.kestra.core.tasks.log.Log
                    message: "New address assigned: {{ trigger.ip }} (id={{ trigger.addressId }})"
                """
        )
    }
)
public class AddressCreatedTrigger extends AbstractTrigger
    implements PollingTriggerInterface, TriggerOutput<AddressCreatedTrigger.Output> {

    @Schema(
        title = "Base URL of the phpIPAM instance",
        description = "Root URL of the phpIPAM instance, e.g. `https://ipam.example.com`."
    )
    @NotNull
    @PluginProperty(group = "connection")
    private Property<String> baseUrl;

    @Schema(title = "Application ID", description = "The API application identifier configured in phpIPAM.")
    @NotNull
    @PluginProperty(group = "connection")
    private Property<String> appId;

    @Schema(title = "Authentication", description = "App token or username+password authentication.")
    @NotNull
    @PluginProperty(group = "connection")
    private io.kestra.plugin.phpipam.PhpipamAuthentication auth;

    @Schema(title = "Disable TLS certificate validation", description = "Trust all TLS certificates.")
    @Builder.Default
    @PluginProperty(group = "connection")
    private Property<Boolean> insecureTls = Property.ofValue(false);

    @Schema(title = "Subnet ID", description = "Numeric ID of the subnet to watch for new addresses.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> subnetId;

    @Builder.Default
    @Schema(title = "Polling interval", description = "How often to poll the subnet for new addresses.")
    @PluginProperty(group = "advanced")
    private Duration interval = Duration.ofMinutes(5);

    @Override
    public Duration getInterval() {
        return interval;
    }

    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext,
                                        TriggerContext triggerContext) throws Exception {
        var runContext = conditionContext.getRunContext();
        Logger logger = runContext.logger();

        var client = AbstractPhpipamTask.buildClient(runContext, baseUrl, appId, auth, insecureTls);
        var rSubnetId = runContext.render(subnetId).as(String.class).orElseThrow();

        List<Address> current;
        try {
            current = client.get("subnets/" + rSubnetId + "/addresses/",
                new TypeReference<PhpipamEnvelope<List<Address>>>() {});
        } catch (Exception e) {
            logger.warn("Failed to poll phpIPAM subnet {}: {}", rSubnetId, e.getMessage());
            return Optional.empty();
        }

        if (current == null || current.isEmpty()) {
            return Optional.empty();
        }

        // KV key scoped to this trigger to avoid collisions with other triggers in the same namespace
        var kvKey = "phpipam-trigger-" + triggerContext.getFlowId() + "-" + getId();
        var kv = runContext.namespaceKv(triggerContext.getNamespace());

        final Set<String> seenIds = loadSeenIds(kv, kvKey);

        var newAddresses = current.stream()
            .filter(a -> a.getId() != null && !seenIds.contains(a.getId()))
            .toList();

        // Persist the full current set of IDs
        var allIds = current.stream()
            .filter(a -> a.getId() != null)
            .map(Address::getId)
            .collect(Collectors.joining(","));
        try {
            kv.put(kvKey, new KVValueAndMetadata(new KVMetadata(null, (java.time.Instant) null), allIds));
        } catch (Exception e) {
            logger.warn("Failed to persist trigger state for key {}: {}", kvKey, e.getMessage());
        }

        if (newAddresses.isEmpty()) {
            return Optional.empty();
        }

        var first = newAddresses.getFirst();
        logger.info("New address detected in subnet {}: id={}, ip={}", rSubnetId,
            first.getId(), first.getIp());

        var output = Output.builder()
            .addressId(first.getId())
            .ip(first.getIp())
            .hostname(first.getHostname())
            .subnetId(rSubnetId)
            .build();

        return Optional.of(TriggerService.generateExecution(this, conditionContext, triggerContext, output));
    }

    private static Set<String> loadSeenIds(io.kestra.core.storages.kv.KVStore kv, String kvKey) {
        try {
            var stored = kv.getValue(kvKey);
            var raw = stored.map(v -> v.value() != null ? v.value().toString() : "").orElse("");
            if (raw.isBlank()) return Set.of();
            return Arrays.stream(raw.split(","))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            return Set.of();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Address ID", description = "Numeric ID of the newly detected address.")
        private final String addressId;

        @Schema(title = "IP address", description = "The newly assigned IP address.")
        private final String ip;

        @Schema(title = "Hostname", description = "Hostname associated with the address, if any.")
        private final String hostname;

        @Schema(title = "Subnet ID", description = "ID of the subnet where the address was detected.")
        private final String subnetId;
    }

}
