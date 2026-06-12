# Kestra PhpIPAM Plugin

## What

- Provides plugin components under `io.kestra.plugin.phpipam` and sub-packages.
- IPAM resource tasks and triggers for phpIPAM, the open-source IP Address Management application.

## Why

- Teams running self-hosted phpIPAM instances need a way to automate subnet provisioning, address allocation, and VLAN/VRF lifecycle management from Kestra workflows.
- Enables GitOps-style IPAM operations: reserve the next free IP, register a new host, or watch a subnet for newly assigned addresses — all from a Kestra flow.

## How

### Architecture

Single-module plugin. Key packages:

- `io.kestra.plugin.phpipam` — connection/auth abstraction (`AbstractPhpipamTask`, `PhpipamAuthentication`, `PhpipamClient`, `PhpipamEnvelope`, `PhpipamApiException`).
- `io.kestra.plugin.phpipam.ipam.section` — section tasks.
- `io.kestra.plugin.phpipam.ipam.subnet` — subnet tasks.
- `io.kestra.plugin.phpipam.ipam.address` — address tasks and trigger.
- `io.kestra.plugin.phpipam.ipam.vlan` — VLAN tasks.
- `io.kestra.plugin.phpipam.ipam.vrf` — VRF tasks.
- `io.kestra.plugin.phpipam.ipam.model` — JSON model classes (`Section`, `Subnet`, `Address`, `Vlan`, `Vrf`).

### Key Plugin Classes

**Foundation**
- `io.kestra.plugin.phpipam.AbstractPhpipamTask` — base task: connection properties + `buildClient()`.
- `io.kestra.plugin.phpipam.PhpipamClient` — HTTP wrapper; prepends `/api/{appId}/`; unwraps JSON envelope; handles 404 and `success:false`.
- `io.kestra.plugin.phpipam.PhpipamAuthentication` — holds `appToken` OR `username`+`password`.

**Sections** (`io.kestra.plugin.phpipam.ipam.section`)
- `List`, `Get`, `Create`, `Update`, `Delete`

**Subnets** (`io.kestra.plugin.phpipam.ipam.subnet`)
- `List`, `Get`, `Create`, `Update`, `Delete`, `Search`, `FirstFree`

**Addresses** (`io.kestra.plugin.phpipam.ipam.address`)
- `List`, `Get`, `Create`, `Update`, `Delete`, `FirstFree`

**Triggers** (`io.kestra.plugin.phpipam.ipam.address`)
- `NewAddressTrigger` — polls a subnet at a configurable interval; fires one execution per newly detected address (dedup via KV store).

**VLANs** (`io.kestra.plugin.phpipam.ipam.vlan`)
- `List`, `Get`, `Create`, `Update`, `Delete`

**VRFs** (`io.kestra.plugin.phpipam.ipam.vrf`)
- `List`, `Get`, `Create`, `Update`, `Delete`

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
│       ├── section/       (List, Get, Create, Update, Delete)
│       ├── subnet/        (List, Get, Create, Update, Delete, Search, FirstFree)
│       ├── address/       (List, Get, Create, Update, Delete, FirstFree, NewAddressTrigger)
│       ├── vlan/          (List, Get, Create, Update, Delete)
│       ├── vrf/           (List, Get, Create, Update, Delete)
│       └── package-info.java
├── src/test/java/io/kestra/plugin/phpipam/
│   ├── WireMockSupport.java
│   ├── PhpipamAuthTest.java
│   └── ipam/
│       ├── section/SectionTasksTest.java
│       ├── subnet/SubnetTasksTest.java
│       ├── address/AddressTasksTest.java
│       ├── vlan/VlanTasksTest.java
│       └── vrf/VrfTasksTest.java
├── build.gradle
└── README.md
```

## Local rules

- Base the wording on the implemented packages and classes, not on template README text.
- `version` is a reserved property name in Kestra — never use it in tasks.
- phpIPAM returns `success:false` on HTTP 200 for failures; `PhpipamClient` maps these to `PhpipamApiException`.
- Task classes named `List` must use `java.util.List` fully qualified in the same file to avoid ambiguity.
- `PhpipamClient` intentionally uses `java.net.http.HttpClient` (not the Kestra internal HTTP client) to support trust-all TLS for self-signed certificates on self-hosted instances.

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
- https://phpipam.net/api/api_reference/
