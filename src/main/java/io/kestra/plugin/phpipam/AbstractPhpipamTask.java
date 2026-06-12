package io.kestra.plugin.phpipam;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractPhpipamTask extends Task {

    @Schema(
        title = "Base URL of the phpIPAM instance",
        description = "Root URL of the phpIPAM instance, e.g. `https://ipam.example.com`. No trailing slash needed."
    )
    @NotNull
    @PluginProperty(group = "connection")
    private Property<String> baseUrl;

    @Schema(
        title = "Application ID",
        description = """
            The API application identifier configured in phpIPAM under Administration → API.
            It appears in the path segment `/api/{appId}/`.
            """
    )
    @NotNull
    @PluginProperty(group = "connection")
    private Property<String> appId;

    @Schema(
        title = "Authentication",
        description = """
            Provide either `appToken` for static App-token authentication,
            or `username` + `password` to obtain a per-run session token.
            Supplying both, or neither, raises a validation error.
            """
    )
    @NotNull
    @PluginProperty(group = "connection")
    private PhpipamAuthentication auth;

    @Schema(
        title = "Disable TLS certificate validation",
        description = """
            Set to `true` to trust all TLS certificates, including self-signed ones.
            Intended for development or internal deployments only.
            """
    )
    @Builder.Default
    @PluginProperty(group = "connection")
    private Property<Boolean> insecureTls = Property.ofValue(false);

    public PhpipamClient buildClient(RunContext runContext) throws Exception {
        return buildClient(runContext, baseUrl, appId, auth, insecureTls);
    }

    public static PhpipamClient buildClient(RunContext runContext,
                                            Property<String> baseUrl,
                                            Property<String> appId,
                                            PhpipamAuthentication auth,
                                            Property<Boolean> insecureTls) throws Exception {
        var rBaseUrl = runContext.render(baseUrl).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("baseUrl is required"));
        var rAppId = runContext.render(appId).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("appId is required"));
        var rInsecureTls = runContext.render(insecureTls).as(Boolean.class).orElse(false);

        var appToken = auth.getAppToken();
        var username = auth.getUsername();
        var password = auth.getPassword();

        boolean hasAppToken = appToken != null
            && runContext.render(appToken).as(String.class).map(s -> !s.isBlank()).orElse(false);
        boolean hasUserPass = username != null && password != null;

        if (hasAppToken && hasUserPass) {
            throw new IllegalArgumentException(
                "phpIPAM auth: provide either appToken OR username+password, not both");
        }
        if (!hasAppToken && !hasUserPass) {
            throw new IllegalArgumentException(
                "phpIPAM auth: provide either appToken or username+password");
        }

        String token;
        if (hasAppToken) {
            token = runContext.render(appToken).as(String.class).orElseThrow();
        } else {
            var rUsername = runContext.render(username).as(String.class).orElseThrow();
            var rPassword = runContext.render(password).as(String.class).orElseThrow();
            token = PhpipamClient.acquireSessionToken(rBaseUrl, rAppId, rUsername, rPassword, rInsecureTls);
        }

        return new PhpipamClient(rBaseUrl, rAppId, token, rInsecureTls);
    }
}
