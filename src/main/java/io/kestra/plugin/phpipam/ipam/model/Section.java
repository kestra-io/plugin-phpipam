package io.kestra.plugin.phpipam.ipam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("masterSection") private String masterSection;
    @JsonProperty("permissions") private String permissions;
    @JsonProperty("strictMode") private String strictMode;
    @JsonProperty("subnetOrdering") private String subnetOrdering;
    @JsonProperty("order") private String order;
    @JsonProperty("editDate") private String editDate;
    @JsonProperty("showVLAN") private String showVlan;
    @JsonProperty("showVRF") private String showVrf;
    @JsonProperty("showSupernetOnly") private String showSupernetOnly;
    @JsonProperty("DNS") private String dns;
}
