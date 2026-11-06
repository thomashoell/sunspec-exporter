# sunspec-exporter - Prometheus exporter for SunSpec PV inverters

[![CI](https://github.com/thomashoell/sunspec-exporter/actions/workflows/release.yml/badge.svg?branch=main)](https://github.com/thomashoell/sunspec-exporter/actions/workflows/release.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/thomashoell/sunspec-exporter/badge)](https://scorecard.dev/viewer/?uri=github.com/thomashoell/sunspec-exporter)

*sunspec-exporter* reads [SunSpec](https://sunspec.org/) compatible PV inverters (e.g. Fronius, SMA, SolarEdge, Kostal) via Modbus TCP and exposes their values as Prometheus metrics.

Supported SunSpec models:

| Model | Content |
|---|---|
| 1 | Common: manufacturer, model, serial number, firmware version (`sunspec_info`) |
| 101, 102, 103 | Single, split and three phase inverter: AC current, voltage, power, frequency, energy; DC values, temperatures, operating state |
| 160 | Multiple MPPT inverter extension: current, voltage, power and energy of each DC input |

Only these models are read: one Modbus block read per model and poll. Points the device doesn't implement (e.g. phase B and C of a single phase inverter) are not exposed instead of being reported as `0`.

## Configuration

Create a `config.yaml` (see [config.yaml.template](config.yaml.template)) in the working directory, or point `SUNSPEC_EXPORTER_CONFIG_FILE` to it. The container image reads `/etc/sunspec-exporter/config.yaml`.

```yaml
modbus:
  poll-interval: 10s   # default
  timeout: 3s          # default, per Modbus request
  devices:
    - host: 192.168.0.10
      port: 502        # default
      unit-id: 126     # default
```

Every setting can also be set as environment variable (Spring Boot relaxed binding), e.g. `MODBUS_DEVICES_0_HOST=192.168.0.10`.

All devices are polled in parallel every `poll-interval`; Prometheus always gets the values of the latest poll. Unreachable devices (e.g. inverters that switch off Modbus at night) are reported with `sunspec_up 0` and reconnected on the next poll.

## Endpoints

| Path | |
|---|---|
| `/actuator/prometheus` | metrics (port 8080) |
| `/actuator/health/liveness`, `/actuator/health/readiness` | Kubernetes probes |
| `/actuator/info` | version |

## Metrics

All metrics have a `host` label (the configured host).

| Metric | Type | Labels | |
|---|---|---|---|
| `sunspec_up` | gauge | | 1 if the latest poll succeeded |
| `sunspec_poll_duration_seconds` | gauge | | duration of the latest poll |
| `sunspec_last_poll_success_timestamp_seconds` | gauge | | Unix time of the latest successful poll |
| `sunspec_info` | info | `manufacturer`, `model`, `serial`, `version`, `sunspec_model` | device information (kept while the device is down) |
| `sunspec_operating_state` | state set | `sunspec_operating_state` | `OFF`, `SLEEPING`, `STARTING`, `MPPT`, `THROTTLED`, `SHUTTING_DOWN`, `FAULT`, `STANDBY` |
| `sunspec_ac_current_amperes` | gauge | `phase`: `total`, `a`, `b`, `c` | |
| `sunspec_ac_voltage_volts` | gauge | `phase`: `ab`, `bc`, `ca`, `an`, `bn`, `cn` | |
| `sunspec_ac_power_watts` | gauge | | |
| `sunspec_ac_apparent_power_voltamperes` | gauge | | |
| `sunspec_ac_reactive_power_voltamperes_reactive` | gauge | | |
| `sunspec_ac_frequency_hertz` | gauge | | |
| `sunspec_ac_energy_watthours_total` | counter | | lifetime energy |
| `sunspec_dc_current_amperes`, `sunspec_dc_voltage_volts`, `sunspec_dc_power_watts` | gauge | | DC side, all inputs |
| `sunspec_temperature_celsius` | gauge | `sensor`: `cabinet`, `heat_sink`, `transformer`, `other` | |
| `sunspec_mppt_current_amperes`, `sunspec_mppt_voltage_volts`, `sunspec_mppt_power_watts` | gauge | `input` (0-based) | per DC input (model 160) |
| `sunspec_mppt_energy_watthours_total` | counter | `input` | lifetime energy per DC input |

Besides these, the usual JVM, process and HTTP metrics of Micrometer are exposed.

### Migrating from 0.x

1.0.0 renamed the metrics to follow the [Prometheus naming conventions](https://prometheus.io/docs/practices/naming/):

| 0.x | 1.x |
|---|---|
| `sunspec_output_current_ampere{type="ac",phase="combined"\|"A"\|...}` | `sunspec_ac_current_amperes{phase="total"\|"a"\|...}` |
| `sunspec_output_voltage_volts{type="ac",phase="AB"\|...}` | `sunspec_ac_voltage_volts{phase="ab"\|...}` |
| `sunspec_output_power_watts` | `sunspec_ac_power_watts` |
| `sunspec_frequency_hertz` | `sunspec_ac_frequency_hertz` |
| `sunspec_power_produced_watt_hours_total` (gauge) | `sunspec_ac_energy_watthours_total` (counter) |
| `sunspec_temperature_celsius{unit=...}` | `sunspec_temperature_celsius{sensor=...}` |
| `sunspec_input_current_ampere{type="dc",input}` | `sunspec_mppt_current_amperes{input}` |
| `sunspec_input_voltage_volts{type="dc",input}` | `sunspec_mppt_voltage_volts{input}` |
| `sunspec_input_power_watts{type="dc",input}` | `sunspec_mppt_power_watts{input}` |
| `sunspec_info{...,mode}` | `sunspec_info{...,sunspec_model}` and `sunspec_operating_state` |

The configuration (`modbus.devices`, `SUNSPEC_EXPORTER_CONFIG_FILE`) is compatible; `modbus.poll-interval`, `modbus.timeout` and `modbus.devices[].unit-id` are new.

## Running

### Docker

Multi-arch images (`linux/amd64`, `linux/arm64`) are published to GHCR:

```bash
docker run -p 8080:8080 -v "$PWD/config.yaml:/etc/sunspec-exporter/config.yaml:ro" \
  ghcr.io/thomashoell/sunspec-exporter:1
```

Tags: `x.y.z`, `x.y`, `x` and `latest`. The image is based on `distroless/java21` and runs as the non-root user `65532`.

### Jar

The jar is attached to each [GitHub release](https://github.com/thomashoell/sunspec-exporter/releases) and requires Java 21 or later:

```bash
java -jar sunspec-exporter-1.0.0.jar
```

### Verifying releases

Jar and image have [build provenance attestations](https://docs.github.com/en/actions/security-for-github-actions/using-artifact-attestations), the image is also signed keyless with [Sigstore](https://www.sigstore.dev/):

```bash
gh attestation verify sunspec-exporter-1.0.0.jar -R thomashoell/sunspec-exporter
gh attestation verify oci://ghcr.io/thomashoell/sunspec-exporter:1.0.0 -R thomashoell/sunspec-exporter

cosign verify ghcr.io/thomashoell/sunspec-exporter:1.0.0 \
  --certificate-identity "https://github.com/thomashoell/sunspec-exporter/.github/workflows/release.yml@refs/heads/main" \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com
```

## Development

Requires Java 21+ and Maven 3.9+.

```bash
mvn verify          # build, unit tests and Spring Boot tests against a simulated inverter
java -jar target/sunspec-exporter.jar
```

The tests use a Modbus TCP slave that serves the registers of a SunSpec inverter (`SunSpecSimulator`), so no hardware is needed.

CI lints the workflows ([actionlint](https://github.com/rhysd/actionlint)) and the Dockerfile ([hadolint](https://github.com/hadolint/hadolint)). To run these checks before each commit, install [pre-commit](https://pre-commit.com) and enable the hooks once per clone: `pre-commit install`.

### Releases

Pull requests are squash-merged; the PR title has to be a [Conventional Commit](https://www.conventionalcommits.org/) (checked by CI). On every push to `main`, [semantic-release](https://github.com/semantic-release/semantic-release) derives the next version from the commits (`fix:` → patch, `feat:` → minor, `!`/`BREAKING CHANGE` → major, `build(deps):` → patch), tags it and creates the GitHub release with the release notes; then the jar and the image are published.
