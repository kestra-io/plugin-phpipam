package io.kestra.plugin.phpipam.ipam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vrf {
    @JsonProperty("vrfId") private String vrfId;
    @JsonProperty("name") private String name;
    @JsonProperty("rd") private String rd;
    @JsonProperty("description") private String description;
    @JsonProperty("editDate") private String editDate;
    @JsonProperty("sections") private String sections;
}
