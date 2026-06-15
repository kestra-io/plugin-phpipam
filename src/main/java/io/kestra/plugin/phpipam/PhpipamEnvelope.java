package io.kestra.plugin.phpipam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the common JSON envelope returned by every phpIPAM REST API call:
 * {@code { "code": 200, "success": true, "message": "...", "data": ... }}
 * <p>
 * On creation (HTTP 201), phpIPAM also includes a top-level {@code "id"} field
 * holding the numeric id of the newly created resource, separate from {@code data}.
 * <p>
 * {@code success:false} with a 2xx HTTP status is a valid API-level failure
 * and must be treated as an error.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhpipamEnvelope<T> {

    @JsonProperty("code")
    private int code;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    // phpIPAM may return the id as a JSON integer (e.g. {"id": 99}) or a string.
    // The explicit setter coerces both forms to String so callers always get a String.
    private String id;

    @JsonSetter("id")
    public void setId(Object id) {
        this.id = id == null ? null : String.valueOf(id);
    }

    @JsonProperty("data")
    private T data;
}
