package com.dreitier.sunspecexporter.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import com.dreitier.sunspecexporter.sunspec.DeviceInfo;
import com.dreitier.sunspecexporter.sunspec.DeviceReading;
import com.dreitier.sunspecexporter.sunspec.DeviceState;
import com.dreitier.sunspecexporter.sunspec.InverterValue;
import com.dreitier.sunspecexporter.sunspec.MpptInput;
import com.dreitier.sunspecexporter.sunspec.SunSpecClient;
import com.dreitier.sunspecexporter.sunspec.SunSpecDevice;
import io.prometheus.metrics.config.EscapingScheme;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import org.junit.jupiter.api.Test;

class SunSpecCollectorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-21T12:00:00Z"), ZoneOffset.UTC);
    private static final DeviceInfo INFO = new DeviceInfo("Fronius", "Symo 8.2-3-M", "0.3.30.2", "12345678", 103);

    private static SunSpecDevice device(String host, DeviceReading reading) {
        SunSpecClient client = new SunSpecClient() {
            @Override
            public DeviceReading read() throws Exception {
                if (reading == null) {
                    throw new IOException("connection refused");
                }
                return reading;
            }

            @Override
            public void close() {
            }
        };
        var device = new SunSpecDevice(new ModbusProperties.Device(host, 502, 126), client, CLOCK);
        device.poll();
        return device;
    }

    private static String scrape(SunSpecDevice... devices) throws IOException {
        var out = new ByteArrayOutputStream();
        PrometheusTextFormatWriter.create().write(out, new SunSpecCollector(List.of(devices)).collect(),
                EscapingScheme.ALLOW_UTF8);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void exposesAllValuesOfAReachableDevice() throws IOException {
        var reading = new DeviceReading(INFO,
                Map.of(InverterValue.AC_POWER, 2840.0,
                        InverterValue.AC_CURRENT_TOTAL, 12.34,
                        InverterValue.AC_CURRENT_A, 4.11,
                        InverterValue.AC_ENERGY, 1234560.0,
                        InverterValue.TEMPERATURE_HEAT_SINK, 45.2),
                "MPPT",
                List.of(new MpptInput(0, "String 1", 5.0, 600.0, 1500.0, 10000.0),
                        new MpptInput(1, "String 2", 5.01, null, null, null)));

        String metrics = scrape(device("inverter", reading));

        assertThat(metrics).contains(
                "sunspec_up{host=\"inverter\"} 1.0",
                "sunspec_poll_duration_seconds{host=\"inverter\"} 0.0",
                "sunspec_last_poll_success_timestamp_seconds{host=\"inverter\"} 1.7820432E9",
                "sunspec_info{host=\"inverter\",manufacturer=\"Fronius\",model=\"Symo 8.2-3-M\","
                        + "serial=\"12345678\",sunspec_model=\"103\",version=\"0.3.30.2\"} 1",
                "sunspec_operating_state{host=\"inverter\",sunspec_operating_state=\"MPPT\"} 1",
                "sunspec_operating_state{host=\"inverter\",sunspec_operating_state=\"SLEEPING\"} 0",
                "# TYPE sunspec_ac_power_watts gauge",
                "sunspec_ac_power_watts{host=\"inverter\"} 2840.0",
                "sunspec_ac_current_amperes{host=\"inverter\",phase=\"total\"} 12.34",
                "sunspec_ac_current_amperes{host=\"inverter\",phase=\"a\"} 4.11",
                "# TYPE sunspec_ac_energy_watthours_total counter",
                "sunspec_ac_energy_watthours_total{host=\"inverter\"} 1234560.0",
                "sunspec_temperature_celsius{host=\"inverter\",sensor=\"heat_sink\"} 45.2",
                "sunspec_mppt_current_amperes{host=\"inverter\",input=\"0\"} 5.0",
                "sunspec_mppt_current_amperes{host=\"inverter\",input=\"1\"} 5.01",
                "sunspec_mppt_voltage_volts{host=\"inverter\",input=\"0\"} 600.0",
                "sunspec_mppt_energy_watthours_total{host=\"inverter\",input=\"0\"} 10000.0");
        // not implemented by the device
        assertThat(metrics)
                .doesNotContain("phase=\"b\"")
                .doesNotContain("sunspec_dc_")
                .doesNotContain("sunspec_mppt_voltage_volts{host=\"inverter\",input=\"1\"}");
    }

    @Test
    void unreachableDeviceOnlyHasUpAndPollMetrics() throws IOException {
        String metrics = scrape(device("offline", null));

        assertThat(metrics)
                .contains("sunspec_up{host=\"offline\"} 0.0",
                        "sunspec_poll_duration_seconds{host=\"offline\"} 0.0")
                .doesNotContain("sunspec_info")
                .doesNotContain("sunspec_last_poll_success")
                .doesNotContain("sunspec_operating_state")
                .doesNotContain("sunspec_ac_");
    }

    @Test
    void devicesAreDistinguishedByHost() throws IOException {
        var reading = new DeviceReading(INFO, Map.of(InverterValue.AC_POWER, 100.0), null, List.of());

        String metrics = scrape(device("inverter-1", reading), device("inverter-2", null));

        assertThat(metrics).contains(
                "sunspec_up{host=\"inverter-1\"} 1.0",
                "sunspec_up{host=\"inverter-2\"} 0.0",
                "sunspec_ac_power_watts{host=\"inverter-1\"} 100.0");
        assertThat(metrics).doesNotContain("sunspec_operating_state");
    }

    @Test
    void namesMatchTheCollectedMetrics() {
        var reading = new DeviceReading(INFO, Map.of(InverterValue.AC_POWER, 100.0), "OFF",
                List.of(new MpptInput(0, "", 1.0, 1.0, 1.0, 1.0)));
        var collector = new SunSpecCollector(List.of(device("inverter", reading)));

        assertThat(collector.getPrometheusNames())
                .containsAll(collector.collect().stream().map(s -> s.getMetadata().getPrometheusName()).toList());
    }

    @Test
    void stateBeforeFirstPollIsDown() {
        var device = new SunSpecDevice(new ModbusProperties.Device("new", 502, 126), null, CLOCK);
        assertThat(device.state()).isEqualTo(new DeviceState(false, null, null, null, null));
    }
}
