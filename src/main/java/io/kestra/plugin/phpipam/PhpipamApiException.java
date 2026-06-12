package io.kestra.plugin.phpipam;

/**
 * Thrown when the phpIPAM API returns an application-level error
 * (success:false in the JSON envelope, or a non-2xx HTTP status code).
 */
public class PhpipamApiException extends RuntimeException {

    private final int httpCode;

    public PhpipamApiException(int httpCode, String message) {
        super("phpIPAM API error [HTTP %d]: %s".formatted(httpCode, message));
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
