package com.dreitier.sunspecexporter.sunspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import org.junit.jupiter.api.Test;

class SunSpecDeviceTest {
    static final DeviceInfo INFO = new DeviceInfo("Fronius", "Symo", "1.0", "42", 103);
    static final DeviceReading READING = new DeviceReading(INFO, Map.of(InverterValue.AC_POWER, 1000.0), "MPPT",
            List.of());

    /** Returns the queued results in order; an exception is thrown. */
    static final class FakeClient implements SunSpecClient {
        private final java.util.ArrayDeque<Object> results = new java.util.ArrayDeque<>();
        boolean closed;

        FakeClient then(Object result) {
            results.add(result);
            return this;
        }

        @Override
        public DeviceReading read() throws Exception {
            Object result = results.isEmpty() ? new java.io.IOException("no result") : results.poll();
            if (result instanceof Exception e) {
                throw e;
            }
            return (DeviceReading) result;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-21T12:00:00Z"), ZoneOffset.UTC);
    private final ModbusProperties.Device config = new ModbusProperties.Device("inverter", 502, 126);

    @Test
    void isDownBeforeTheFirstPoll() {
        var device = new SunSpecDevice(config, new FakeClient(), clock);

        assertThat(device.host()).isEqualTo("inverter");
        assertThat(device.state()).isEqualTo(DeviceState.INITIAL);
    }

    @Test
    void keepsInfoAndLastSuccessWhileDown() {
        var client = new FakeClient().then(READING).then(new java.io.IOException("connection refused")).then(READING);
        var device = new SunSpecDevice(config, client, clock);

        device.poll();
        assertThat(device.state()).isEqualTo(
                new DeviceState(true, READING, INFO, Duration.ZERO, clock.instant()));

        device.poll();
        assertThat(device.state()).isEqualTo(
                new DeviceState(false, null, INFO, Duration.ZERO, clock.instant()));

        device.poll();
        assertThat(device.state().up()).isTrue();
        assertThat(device.state().reading()).isSameAs(READING);
    }

    @Test
    void neverConnectedDeviceHasNoInfo() {
        var device = new SunSpecDevice(config, new FakeClient(), clock);

        device.poll();
        device.poll();

        assertThat(device.state()).isEqualTo(new DeviceState(false, null, null, Duration.ZERO, null));
    }

    @Test
    void closeClosesTheClient() {
        var client = new FakeClient();
        new SunSpecDevice(config, client, clock).close();

        assertThat(client.closed).isTrue();
    }
}
