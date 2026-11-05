package com.dreitier.sunspecexporter.sunspec;

/**
 * A DC input (MPP tracker) from the SunSpec multiple MPPT inverter extension model (160).
 * Values the device doesn't implement are {@code null}.
 *
 * @param index  0-based position of the input in the model
 * @param label  input ID string of the device, may be empty
 * @param energy lifetime energy in Wh
 */
public record MpptInput(
        int index,
        String label,
        Double current,
        Double voltage,
        Double power,
        Double energy) {
}
