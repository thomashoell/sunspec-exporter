package com.dreitier.sunspecexporter.sunspec;

/**
 * Static information from the SunSpec common model (1).
 *
 * @param inverterModel the SunSpec inverter model the device implements (101, 102 or 103)
 */
public record DeviceInfo(
        String manufacturer,
        String model,
        String version,
        String serialNumber,
        int inverterModel) {
}
