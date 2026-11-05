package com.dreitier.sunspecexporter.sunspec;

/**
 * Operating state (St) of the SunSpec inverter models 101-103.
 */
public enum OperatingState {
    OFF, SLEEPING, STARTING, MPPT, THROTTLED, SHUTTING_DOWN, FAULT, STANDBY;

    /**
     * @param value raw value of the enum16 point (1-8)
     * @return the state, {@code null} if not implemented or unknown
     */
    static OperatingState of(int value) {
        return value >= 1 && value <= values().length ? values()[value - 1] : null;
    }
}
