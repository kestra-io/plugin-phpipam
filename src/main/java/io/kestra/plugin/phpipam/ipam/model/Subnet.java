package io.kestra.plugin.phpipam.ipam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(title = "Usage", description = "Address usage reported by phpIPAM for this subnet. Only set by `Get`, and null when the usage request fails.")
    @JsonProperty("usage") private SubnetUsage usage;

    public void setUsage(SubnetUsage usage) {
        this.usage = usage;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubnetUsage {
        @Schema(title = "Used addresses")
        @JsonProperty("used") private String used;
        @Schema(title = "Maximum hosts")
        @JsonProperty("maxhosts") private String maxHosts;
        @Schema(title = "Free hosts")
        @JsonProperty("freehosts") private String freeHosts;
        @Schema(title = "Free hosts percent")
        @JsonProperty("freehosts_percent") private String freeHostsPercent;
        @Schema(title = "Offline percent")
        @JsonProperty("Offline_percent") private String offlinePercent;
    }
}
