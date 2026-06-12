# Kestra PhpIPAM Plugin

## What

- Provides plugin components under `io.kestra.plugin.phpipam` and `io.kestra.plugin.phpipam.ipam`.
- IPAM resource tasks and triggers for phpIPAM, the open-source IP Address Management application.

## Why

- Teams running self-hosted phpIPAM instances need a way to automate subnet provisioning, address allocation, and VLAN/VRF lifecycle management from Kestra workflows.
- Enables GitOps-style IPAM operations: reserve the next free IP, register a new host, or watch a subnet for newly assigned addresses — all from a Kestra flow.

## How

### Architecture

Single-module plugin. Key packages:

- `io.kestra.plugin.phpipam` — connection/auth abstraction (`AbstractPhpipamTask`, `PhpipamAuthentication`, `PhpipamClient`, `PhpipamEnvelope`, `PhpipamApiException`).
- `io.kestra.plugin.phpipam.ipam` — all resource tasks and the polling trigger.
- `io.kestra.plugin.phpipam.ipam.model` — JSON model classes (`Section`, `Subnet`, `Address`, `Vlan`, `Vrf`).

### Key Plugin Classes

**Foundation**
- `io.kestra.plugin.phpipam.AbstractPhpipamTask` — base task: connection properties + `buildClient()`.
- `io.kestra.plugin.phpipam.PhpipamClient` — HTTP wrapper; prepends `/api/{appId}/`; unwraps JSON envelope; handles 404 and `success:false`.
- `io.kestra.plugin.phpipam.PhpipamAuthentication` — holds `appToken` OR `username`+`password`.

**Sections**
- `io.kestra.plugin.phpipam.ipam.SectionList`
- `io.kestra.plugin.phpipam.ipam.SectionGet`
- `io.kestra.plugin.phpipam.ipam.SectionCreate`
- `io.kestra.plugin.phpipam.ipam.SectionUpdate`
- `io.kestra.plugin.phpipam.ipam.SectionDelete`

**Subnets**
- `io.kestra.plugin.phpipam.ipam.SubnetList`
- `io.kestra.plugin.phpipam.ipam.SubnetGet`
- `io.kestra.plugin.phpipam.ipam.SubnetCreate`
- `io.kestra.plugin.phpipam.ipam.SubnetUpdate`
- `io.kestra.plugin.phpipam.ipam.SubnetDelete`
- `io.kestra.plugin.phpipam.ipam.SubnetSearch` — search by CIDR
- `io.kestra.plugin.phpipam.ipam.SubnetFirstFree` — first free child subnet

**Addresses**
- `io.kestra.plugin.phpipam.ipam.AddressList`
- `io.kestra.plugin.phpipam.ipam.AddressGet`
- `io.kestra.plugin.phpipam.ipam.AddressCreate`
- `io.kestra.plugin.phpipam.ipam.AddressUpdate`
- `io.kestra.plugin.phpipam.ipam.AddressDelete`
- `io.kestra.plugin.phpipam.ipam.AddressFirstFree` — first free IP in a subnet

**VLANs**
- `io.kestra.plugin.phpipam.ipam.VlanList`
- `io.kestra.plugin.phpipam.ipam.VlanGet`
- `io.kestra.plugin.phpipam.ipam.VlanCreate`
- `io.kestra.plugin.phpipam.ipam.VlanUpdate`
- `io.kestra.plugin.phpipam.ipam.VlanDelete`

**VRFs**
- `io.kestra.plugin.phpipam.ipam.VrfList`
- `io.kestra.plugin.phpipam.ipam.VrfGet`
- `io.kestra.plugin.phpipam.ipam.VrfCreate`
- `io.kestra.plugin.phpipam.ipam.VrfUpdate`
- `io.kestra.plugin.phpipam.ipam.VrfDelete`

**Triggers**
- `io.kestra.plugin.phpipam.ipam.AddressCreatedTrigger` — polls a subnet at a configurable interval; fires on newly detected addresses (dedup via trigger state).

### Project Structure

```
plugin-phpipam/
├── src/main/java/io/kestra/plugin/phpipam/
│   ├── AbstractPhpipamTask.java
│   ├── PhpipamAuthentication.java
│   ├── PhpipamApiException.java
│   ├── PhpipamClient.java
│   ├── PhpipamEnvelope.java
│   ├── package-info.java
│   └── ipam/
│       ├── model/         (Section, Subnet, Address, Vlan, Vrf)
│       ├── Section*.java
│       ├── Subnet*.java
│       ├── Address*.java
│       ├── Vlan*.java
│       ├── Vrf*.java
│       ├── AddressCreatedTrigger.java
│       └── package-info.java
├── src/test/java/io/kestra/plugin/phpipam/
│   ├── WireMockSupport.java
│   ├── PhpipamAuthTest.java
│   └── ipam/
│       ├── SectionTasksTest.java
│       ├── SubnetTasksTest.java
│       ├── AddressTasksTest.java
│       ├── VlanTasksTest.java
│       └── VrfTasksTest.java
├── build.gradle
└── README.md
```

## Local rules

- Base the wording on the implemented packages and classes, not on template README text.
- `version` is a reserved property name in Kestra — never use it in tasks.
- phpIPAM returns `success:false` on HTTP 200 for failures; `PhpipamClient` maps these to `PhpipamApiException`.

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
- https://phpipam.net/api/api_reference/
