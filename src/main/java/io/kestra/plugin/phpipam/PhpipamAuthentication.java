package io.kestra.plugin.phpipam;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Authentication modes supported by the phpIPAM REST API.
 * Provide either {@code appToken} (static App token) or both {@code username} and {@code password}
 * (user-based session token obtained via POST /api/{appId}/user/).
 * Supplying both or neither raises a validation error at run time.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PhpipamAuthentication {

    @Schema(
        title = "Static App token",
        description = """
            A token generated in the phpIPAM administration panel under Administration → API.
            This is set as the `X-App-Token` request header.
            Mutually exclusive with `username` / `password`.
            """
    )
    private io.kestra.core.models.property.Property<String> appToken;

    @Schema(
        title = "Username",
        description = """
            phpIPAM username used to obtain a per-session token via `POST /api/{appId}/user/`.
            Must be provided together with `password`.
            Mutually exclusive with `appToken`.
            """
    )
    private io.kestra.core.models.property.Property<String> username;

    @Schema(
        title = "Password",
        description = "Password for the phpIPAM user. Must be provided together with `username`."
    )
    private io.kestra.core.models.property.Property<String> password;
}
