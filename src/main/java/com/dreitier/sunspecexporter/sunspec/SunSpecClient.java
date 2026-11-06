package com.dreitier.sunspecexporter.sunspec;

/**
 * Reads the values of one SunSpec device.
 */
public interface SunSpecClient extends AutoCloseable {

    /**
     * Reads the current values, (re-)connecting to the device if necessary.
     *
     * @throws Exception if the device is not reachable or doesn't support the required SunSpec models
     */
    DeviceReading read() throws Exception;

    /** Closes the connection; the next {@link #read()} reconnects. */
    @Override
    void close();
}
