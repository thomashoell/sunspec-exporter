package com.dreitier.sunspecexporter.sunspec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Values read from a SunSpec device in one poll.
 *
 * @param inverter       the implemented values of the inverter model; values the device doesn't implement are absent
 * @param operatingState name of the operating state (e.g. {@code MPPT}), {@code null} if unknown
 * @param mpptInputs     DC inputs, empty if the device doesn't implement model 160
 */
public record DeviceReading(
        DeviceInfo info,
        Map<InverterValue, Double> inverter,
        String operatingState,
        List<MpptInput> mpptInputs) {

    public DeviceReading {
        inverter = inverter.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(inverter));
        mpptInputs = List.copyOf(mpptInputs);
    }
}
