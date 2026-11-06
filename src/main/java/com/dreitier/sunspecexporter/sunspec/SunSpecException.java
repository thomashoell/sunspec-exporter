package com.dreitier.sunspecexporter.sunspec;

/**
 * The device doesn't behave like a supported SunSpec device.
 */
public class SunSpecException extends Exception {

    public SunSpecException(String message) {
        super(message);
    }
}
