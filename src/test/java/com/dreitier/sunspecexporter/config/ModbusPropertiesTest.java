package com.dreitier.sunspecexporter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ModbusPropertiesTest {

    private static ModbusProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bindOrCreate("modbus", ModbusProperties.class);
    }

    @Test
    void defaults() {
        var properties = bind(Map.of("modbus.devices[0].host", "192.168.0.10"));

        assertThat(properties.pollInterval()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.devices()).containsExactly(new ModbusProperties.Device("192.168.0.10", 502, 126));
    }

    @Test
    void noDevices() {
        assertThat(bind(Map.of()).devices()).isEmpty();
    }

    @Test
    void customValues() {
        var properties = bind(Map.of(
                "modbus.poll-interval", "30s",
                "modbus.timeout", "500ms",
                "modbus.devices[0].host", "inverter-1",
                "modbus.devices[0].port", "1502",
                "modbus.devices[0].unit-id", "1",
                "modbus.devices[1].host", "inverter-2"));

        assertThat(properties.pollInterval()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.timeout()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.devices()).containsExactly(
                new ModbusProperties.Device("inverter-1", 1502, 1),
                new ModbusProperties.Device("inverter-2", 502, 126));
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> new ModbusProperties(Duration.ZERO, Duration.ofSeconds(1), List.of()))
                .hasMessageContaining("poll-interval");
        assertThatThrownBy(() -> new ModbusProperties(Duration.ofSeconds(1), Duration.ofSeconds(-1), List.of()))
                .hasMessageContaining("timeout");
        assertThatThrownBy(() -> new ModbusProperties(Duration.ofSeconds(1), Duration.ofSeconds(1),
                List.of(new ModbusProperties.Device("inverter", 502, 1), new ModbusProperties.Device("inverter", 1502, 1))))
                .hasMessageContaining("configured twice");
        assertThatThrownBy(() -> new ModbusProperties.Device(" ", 502, 126)).hasMessageContaining("host");
        assertThatThrownBy(() -> new ModbusProperties.Device(null, 502, 126)).hasMessageContaining("host");
        assertThatThrownBy(() -> new ModbusProperties.Device("host", 0, 126)).hasMessageContaining("port");
        assertThatThrownBy(() -> new ModbusProperties.Device("host", 502, 256)).hasMessageContaining("unit ID");
    }
}
