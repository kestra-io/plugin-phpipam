<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png"  alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

<p align="center">
  <a href="https://twitter.com/kestra_io" style="margin: 0 10px;">
        <img src="https://kestra.io/twitter.svg" alt="twitter" width="35" height="25" /></a>
  <a href="https://www.linkedin.com/company/kestra/" style="margin: 0 10px;">
        <img src="https://kestra.io/linkedin.svg" alt="linkedin" width="35" height="25" /></a>
  <a href="https://www.youtube.com/@kestra-io" style="margin: 0 10px;">
        <img src="https://kestra.io/youtube.svg" alt="youtube" width="35" height="25" /></a>
</p>

<br />
<p align="center">
    <a href="https://go.kestra.io/video/product-overview" target="_blank">
        <img src="https://kestra.io/startvideo.png" alt="Get started in 3 minutes with Kestra" width="640px" />
    </a>
</p>
<p align="center" style="color:grey;"><i>Get started with Kestra in 3 minutes.</i></p>

# Kestra phpIPAM Plugin

Kestra plugin for [phpIPAM](https://phpipam.net/), the open-source IP Address Management (IPAM) application. Automate subnet provisioning, address allocation, and VLAN/VRF lifecycle management from Kestra workflows.

## What

Tasks and triggers grouped by resource type under `io.kestra.plugin.phpipam.ipam.*`:

| Package | Tasks |
|---|---|
| `ipam.section` | `List`, `Get`, `Create`, `Update`, `Delete` |
| `ipam.subnet` | `List`, `Get`, `Create`, `Update`, `Delete`, `Search`, `FirstFree` |
| `ipam.address` | `List`, `Get`, `Create`, `Update`, `Delete`, `FirstFree`, `NewAddressTrigger` |
| `ipam.vlan` | `List`, `Get`, `Create`, `Update`, `Delete` |
| `ipam.vrf` | `List`, `Get`, `Create`, `Update`, `Delete` |

## Authentication

Two mutually exclusive modes:

| Mode | How to configure |
|---|---|
| Static App token | Set `auth.appToken` to the token from phpIPAM Administration → API. |
| User/password session | Set `auth.username` + `auth.password`; a session token is obtained automatically via `POST /api/{appId}/user/`. |

Both modes accept `{{ secret('...') }}` for credential values.

## Quick start

```yaml
id: allocate_next_ip
namespace: company.team
tasks:
  - id: get_free_ip
    type: io.kestra.plugin.phpipam.ipam.address.FirstFree
    baseUrl: "https://ipam.example.com"
    appId: myapp
    auth:
      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
    subnetId: "10"

  - id: register_ip
    type: io.kestra.plugin.phpipam.ipam.address.Create
    baseUrl: "https://ipam.example.com"
    appId: myapp
    auth:
      appToken: "{{ secret('PHPIPAM_APP_TOKEN') }}"
    subnetId: "10"
    ip: "{{ outputs.get_free_ip.ip }}"
    hostname: "new-server.example.com"
    description: "Provisioned by Kestra"
```

## Self-signed TLS

For self-hosted instances with self-signed certificates, add `insecureTls: true` to any task or trigger. Use only in development or trusted internal environments.

## Documentation

- Full documentation: [kestra.io/docs](https://kestra.io/docs)
- Plugin Developer Guide: [kestra.io/docs/plugin-developer-guide](https://kestra.io/docs/plugin-developer-guide/)

## License

Apache 2.0 © [Kestra Technologies](https://kestra.io)

## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)
