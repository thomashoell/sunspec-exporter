package com.dreitier.sunspecexporter.sunspec;

import com.dreitier.sunspecexporter.config.ModbusProperties;

@FunctionalInterface
public interface SunSpecClientFactory {

    SunSpecClient create(ModbusProperties.Device device);
}
