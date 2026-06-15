package io.kestra.plugin.phpipam.ipam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vlan {
    @JsonProperty("vlanId") private String vlanId;
    @JsonProperty("name") private String name;
    @JsonProperty("number") private String number;
    @JsonProperty("description") private String description;
    @JsonProperty("editDate") private String editDate;
    @JsonProperty("domainId") private String domainId;
}
