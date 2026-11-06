package com.dreitier.sunspecexporter.sunspec;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configured device and the result of its latest poll.
 */
public final class SunSpecDevice implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(SunSpecDevice.class);

    private final ModbusProperties.Device config;
    private final SunSpecClient client;
    private final Clock clock;

    private volatile DeviceState state = DeviceState.INITIAL;

    public SunSpecDevice(ModbusProperties.Device config, SunSpecClient client, Clock clock) {
        this.config = config;
        this.client = client;
        this.clock = clock;
    }

    /** Value of the {@code host} label. */
    public String host() {
        return config.host();
    }

    public DeviceState state() {
        return state;
    }

    /**
     * Reads the device and updates {@link #state()}. Never throws: a failed poll marks the device as down.
     */
    public void poll() {
        Instant start = clock.instant();
        DeviceState previous = state;
        try {
            DeviceReading reading = client.read();
            Instant end = clock.instant();
            state = new DeviceState(true, reading, reading.info(), Duration.between(start, end), end);
            if (!previous.up() && previous.pollDuration() != null) {
                log.info("{} is reachable again", host());
            }
        } catch (Exception e) {
            state = new DeviceState(false, null, previous.info(), Duration.between(start, clock.instant()),
                    previous.lastSuccess());
            // inverters often switch off their Modbus interface at night: log the change, not every failed poll
            if (previous.up() || previous.pollDuration() == null) {
                log.warn("polling {}:{} failed: {}", host(), config.port(), describe(e));
                log.debug("polling {} failed", host(), e);
            } else {
                log.debug("polling {} failed: {}", host(), describe(e));
            }
        }
    }

    private static String describe(Throwable e) {
        var message = new StringBuilder(String.valueOf(e.getMessage()));
        for (Throwable cause = e.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            message.append(": ").append(cause.getMessage());
        }
        return message.toString();
    }

    @Override
    public void close() {
        client.close();
    }
}
