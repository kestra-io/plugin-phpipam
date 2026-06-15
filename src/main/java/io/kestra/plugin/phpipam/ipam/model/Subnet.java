package io.kestra.plugin.phpipam.ipam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subnet {
    @JsonProperty("id") private String id;
    @JsonProperty("subnet") private String subnet;
    @JsonProperty("mask") private String mask;
    @JsonProperty("sectionId") private String sectionId;
    @JsonProperty("description") private String description;
    @JsonProperty("masterSubnetId") private String masterSubnetId;
    @JsonProperty("vlanId") private String vlanId;
    @JsonProperty("vrfId") private String vrfId;
    @JsonProperty("nameserverId") private String nameserverId;
    @JsonProperty("showName") private String showName;
    @JsonProperty("permissions") private String permissions;
    @JsonProperty("editDate") private String editDate;
    @JsonProperty("usage") private SubnetUsage usage;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubnetUsage {
        @JsonProperty("used") private String used;
        @JsonProperty("maxhosts") private String maxHosts;
        @JsonProperty("freehosts") private String freeHosts;
        @JsonProperty("freehosts_percent") private String freeHostsPercent;
        @JsonProperty("Offline_percent") private String offlinePercent;
    }
}
