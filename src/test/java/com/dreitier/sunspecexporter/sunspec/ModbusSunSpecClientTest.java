package com.dreitier.sunspecexporter.sunspec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import org.junit.jupiter.api.Test;

class ModbusSunSpecClientTest {

    private static ModbusSunSpecClient client(SunSpecSimulator simulator) {
        return new ModbusSunSpecClient(
                new ModbusProperties.Device("127.0.0.1", simulator.port(), SunSpecSimulator.UNIT_ID),
                Duration.ofSeconds(2));
    }

    @Test
    void readsThreePhaseInverterWithMpptExtension() throws Exception {
        try (var simulator = SunSpecSimulator.start(SunSpecSimulator.threePhaseInverter());
             var client = client(simulator)) {
            DeviceReading reading = client.read();

            assertThat(reading.info()).isEqualTo(
                    new DeviceInfo("Fronius", "Symo 8.2-3-M", "0.3.30.2", "12345678", 103));
            assertThat(reading.operatingState()).isEqualTo("MPPT");
            assertThat(reading.inverter()).contains(
                    entry(InverterValue.AC_CURRENT_TOTAL, 12.34),
                    entry(InverterValue.AC_CURRENT_B, 4.12),
                    entry(InverterValue.AC_VOLTAGE_AB, 400.5),
                    entry(InverterValue.AC_VOLTAGE_CN, 229.9),
                    entry(InverterValue.AC_POWER, 2840.0),
                    entry(InverterValue.AC_APPARENT_POWER, 2850.0),
                    entry(InverterValue.AC_FREQUENCY, 50.01),
                    entry(InverterValue.AC_ENERGY, 1234560.0),
                    entry(InverterValue.DC_POWER, 2950.0),
                    entry(InverterValue.TEMPERATURE_HEAT_SINK, 45.2));
            // not implemented by the simulated device
            assertThat(reading.inverter()).doesNotContainKeys(
                    InverterValue.AC_REACTIVE_POWER, InverterValue.DC_CURRENT, InverterValue.DC_VOLTAGE,
                    InverterValue.TEMPERATURE_CABINET, InverterValue.TEMPERATURE_OTHER);

            assertThat(reading.mpptInputs()).containsExactly(
                    new MpptInput(0, "String 1", 5.0, 600.0, 1500.0, 10000.0),
                    new MpptInput(1, "String 2", 5.01, 600.1, 1501.0, 10001.0));
        }
    }

    @Test
    void singlePhaseInverterHasNoValuesForPhaseBAndC() throws Exception {
        try (var simulator = SunSpecSimulator.start(SunSpecSimulator.singlePhaseInverter().mpptInputs(0));
             var client = client(simulator)) {
            DeviceReading reading = client.read();

            assertThat(reading.info().inverterModel()).isEqualTo(101);
            assertThat(reading.inverter()).containsEntry(InverterValue.AC_CURRENT_A, 12.34)
                    .containsEntry(InverterValue.AC_VOLTAGE_AN, 230.1)
                    .doesNotContainKeys(InverterValue.AC_CURRENT_B, InverterValue.AC_CURRENT_C,
                            InverterValue.AC_VOLTAGE_AB, InverterValue.AC_VOLTAGE_BN);
            assertThat(reading.mpptInputs()).isEmpty();
        }
    }

    @Test
    void reconnectsAfterTheDeviceWasUnreachable() throws Exception {
        try (var simulator = SunSpecSimulator.start(SunSpecSimulator.threePhaseInverter());
             var client = client(simulator)) {
            assertThat(client.read().operatingState()).isEqualTo("MPPT");

            simulator.stop();
            assertThatThrownBy(client::read).isInstanceOf(Exception.class);

            simulator.open();
            assertThat(client.read().operatingState()).isEqualTo("MPPT");
        }
    }

    @Test
    void failsIfTheDeviceHasNoInverterModel() throws Exception {
        try (var simulator = SunSpecSimulator.start(SunSpecSimulator.commonModelOnly().mpptInputs(0));
             var client = client(simulator)) {
            assertThatThrownBy(client::read)
                    .isInstanceOf(SunSpecException.class)
                    .hasMessageContaining("inverter model");
        }
    }

    @Test
    void failsIfTheDeviceIsUnreachable() throws Exception {
        int port;
        try (var simulator = SunSpecSimulator.start(SunSpecSimulator.threePhaseInverter())) {
            port = simulator.port();
        }
        try (var client = new ModbusSunSpecClient(new ModbusProperties.Device("127.0.0.1", port, 126),
                Duration.ofMillis(500))) {
            assertThatThrownBy(client::read).isInstanceOf(Exception.class);
        }
    }
}
