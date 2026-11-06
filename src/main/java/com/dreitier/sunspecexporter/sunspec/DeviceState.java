package com.dreitier.sunspecexporter.sunspec;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of the latest poll of a device.
 *
 * @param up           whether the latest poll succeeded
 * @param reading      values of the latest poll, {@code null} if it failed
 * @param info         latest known device info, also kept while the device is down; {@code null} if never connected
 * @param pollDuration duration of the latest poll, {@code null} before the first poll
 * @param lastSuccess  time of the latest successful poll, {@code null} if there was none
 */
public record DeviceState(
        boolean up,
        DeviceReading reading,
        DeviceInfo info,
        Duration pollDuration,
        Instant lastSuccess) {

    static final DeviceState INITIAL = new DeviceState(false, null, null, null, null);
}
