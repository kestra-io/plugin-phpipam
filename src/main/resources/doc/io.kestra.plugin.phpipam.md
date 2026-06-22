# How to use the phpIPAM plugin

Manage IP Address Management resources — sections, subnets, IP addresses, VLANs, and VRFs — in a self-hosted [phpIPAM](https://phpipam.net/) instance from Kestra flows.

## Authentication

All tasks and the trigger require `baseUrl` (the phpIPAM root URL, e.g. `https://ipam.example.com`), `appId` (the API application identifier configured under Administration → API), and `auth` — all required. Provide exactly one authentication mode under `auth`:

- `appToken` — a static App token generated in phpIPAM (sent as the `token` header), or
- `username` + `password` — credentials used to obtain a per-run session token.

Supplying both modes, or neither, raises a validation error. Optionally set `insecureTls` (default `false`; set to `true` to trust self-signed certificates — development/internal use only). Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

Tasks are grouped by resource under `ipam.*`. Two conventions apply across them:

- **Identity** — resources are addressed by numeric string IDs. `Get`, `Update`, and `Delete` take the resource's own ID (`sectionId`, `subnetId`, `addressId`, `vlanId`, or `vrfId`); `Update` changes only the fields you supply — the editable fields mirror that resource's optional `Create` fields (for example, `address.Update` accepts `hostname`, `resourceDescription`, and `owner`).
- **`List` tasks** accept `fetchType` (`FETCH`, the default, returns all rows inline; `FETCH_ONE`, `STORE`, or `NONE`). Some require a parent ID to scope the listing (noted below).

### Sections

`ipam.section` — List, Get, Create, Update, Delete. `Create` requires `name`; optional `resourceDescription` and `masterSection` (the parent section ID, for a sub-section). `List` takes no parent ID.

### Subnets

`ipam.subnet` — List, Get, Create, Update, Delete, plus `Search` and `FirstFree`. `List` requires `sectionId`. `Create` requires `subnet` (network address), `mask` (CIDR prefix length), and `sectionId`; optional `resourceDescription`, `vlanId`, `vrfId`. `Search` finds subnets by `cidr` (required; IPv4 CIDR such as `192.168.1.0/24`, validated) and outputs matching `subnets`. `FirstFree` requires `subnetId` (a master subnet) and returns the `cidr` of the first available child-subnet slot, erroring if none exists.

### Addresses

`ipam.address` — List, Get, Create, Update, Delete, plus `FirstFree`. `List` requires `subnetId`. `Create` requires `subnetId` and `ip`; optional `hostname`, `resourceDescription`, `owner`. `FirstFree` requires `subnetId` and returns the first unallocated `ip` in that subnet, erroring if the subnet is full — combine with `Create` to reserve the next available address.

### VLANs

`ipam.vlan` — List, Get, Create, Update, Delete. `Create` requires `name` and `number` (the 802.1Q tag, 1–4094); optional `resourceDescription`.

### VRFs

`ipam.vrf` — List, Get, Create, Update, Delete. `Create` requires `name`; optional `rd` (a BGP Route Distinguisher in `ASN:nn` form, e.g. `65000:1`) and `resourceDescription`.

## Triggers

`ipam.address.NewAddressTrigger` fires when new IP addresses appear in a subnet — set `subnetId` (required). Optionally set `interval` (default `PT5M`). Each poll lists the subnet's addresses and fires one execution per newly detected address (deduplicated by address `id` via the namespace KV store); when several new addresses appear in one cycle, each fires on a successive poll. Outputs `addressId`, `ip`, `hostname`, and `subnetId`.
