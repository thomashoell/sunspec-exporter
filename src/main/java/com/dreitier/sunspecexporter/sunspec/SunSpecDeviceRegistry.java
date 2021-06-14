package com.dreitier.sunspecexporter.sunspec;

import com.dreitier.sunspecexporter.modbus.configuration.ModbusConfiguration;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
@Component
public class SunSpecDeviceRegistry {
    private final ModbusConfiguration modbusConfiguration;
    @Getter
    private final List<SunSpecDevice> sunSpecDevices;

    public SunSpecDeviceRegistry(@NonNull final ModbusConfiguration modbusConfiguration) {
        this.modbusConfiguration = modbusConfiguration;
        this.sunSpecDevices = this.modbusConfiguration.getDevices()
                .stream()
                .map(SunSpecDevice::new)
                .collect(Collectors.toList());
    }
}
