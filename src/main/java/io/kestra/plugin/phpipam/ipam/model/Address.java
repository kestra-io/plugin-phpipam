package io.kestra.plugin.phpipam.ipam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {
    @JsonProperty("id") private String id;
    @JsonProperty("subnetId") private String subnetId;
    @JsonProperty("ip") private String ip;
    @JsonProperty("is_gateway") private String isGateway;
    @JsonProperty("description") private String description;
    @JsonProperty("hostname") private String hostname;
    @JsonProperty("mac") private String mac;
    @JsonProperty("owner") private String owner;
    @JsonProperty("tag") private String tag;
    @JsonProperty("deviceId") private String deviceId;
    @JsonProperty("note") private String note;
    @JsonProperty("editDate") private String editDate;
    @JsonProperty("lastSeen") private String lastSeen;
    @JsonProperty("excludePing") private String excludePing;
    @JsonProperty("PTRignore") private String ptrIgnore;
    @JsonProperty("PTR") private String ptr;
}
