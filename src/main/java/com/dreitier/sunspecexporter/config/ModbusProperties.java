package com.dreitier.sunspecexporter.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the polled SunSpec devices ({@code modbus.*}).
 *
 * @param pollInterval how often all devices are polled
 * @param timeout      connect and read timeout of a single Modbus request
 * @param devices      the SunSpec devices
 */
@ConfigurationProperties("modbus")
public record ModbusProperties(
        @DefaultValue("10s") Duration pollInterval,
        @DefaultValue("3s") Duration timeout,
        @DefaultValue List<Device> devices) {

    public ModbusProperties {
        if (pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("modbus.poll-interval must be positive");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("modbus.timeout must be positive");
        }
        devices = List.copyOf(devices);
        // the host is the label that distinguishes the devices
        Set<String> hosts = new HashSet<>();
        for (Device device : devices) {
            if (!hosts.add(device.host())) {
                throw new IllegalArgumentException("modbus.devices: " + device.host() + " is configured twice");
            }
        }
    }

    /**
     * A SunSpec device reachable via Modbus TCP.
     *
     * @param host   hostname or IP address, also used as {@code host} label
     * @param port   Modbus TCP port
     * @param unitId Modbus unit ID of the SunSpec device
     */
    public record Device(
            String host,
            @DefaultValue("502") int port,
            @DefaultValue("126") int unitId) {

        public Device {
            Objects.requireNonNull(host, "modbus.devices[].host must be set");
            if (host.isBlank()) {
                throw new IllegalArgumentException("modbus.devices[].host must not be blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("invalid Modbus port " + port + " for " + host);
            }
            if (unitId < 0 || unitId > 255) {
                throw new IllegalArgumentException("invalid Modbus unit ID " + unitId + " for " + host);
            }
        }
    }
}
