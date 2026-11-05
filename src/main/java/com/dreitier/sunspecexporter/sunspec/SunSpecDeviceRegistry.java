package com.dreitier.sunspecexporter.sunspec;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The configured devices. All devices are polled in parallel every {@code modbus.poll-interval}, so a slow or
 * unreachable device doesn't delay the others.
 */
@Component
public class SunSpecDeviceRegistry implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SunSpecDeviceRegistry.class);

    private final List<SunSpecDevice> devices;

    public SunSpecDeviceRegistry(ModbusProperties properties, SunSpecClientFactory clientFactory, Clock clock) {
        this.devices = properties.devices().stream()
                .map(device -> new SunSpecDevice(device, clientFactory.create(device), clock))
                .toList();
        if (devices.isEmpty()) {
            log.warn("no devices configured (modbus.devices), see config.yaml.template");
        } else {
            log.info("polling {} device(s) every {}: {}", devices.size(), properties.pollInterval(),
                    devices.stream().map(SunSpecDevice::host).toList());
        }
    }

    public List<SunSpecDevice> devices() {
        return devices;
    }

    @Scheduled(fixedDelayString = "${modbus.poll-interval:10s}")
    public void pollAll() {
        if (devices.size() == 1) {
            devices.getFirst().poll();
            return;
        }
        // close() waits for all polls; each poll is bounded by the Modbus timeout
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            devices.forEach(device -> executor.execute(device::poll));
        }
    }

    @Override
    public void destroy() {
        devices.forEach(SunSpecDevice::close);
    }
}
