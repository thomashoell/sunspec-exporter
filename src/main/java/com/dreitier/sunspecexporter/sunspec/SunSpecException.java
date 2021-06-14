package com.dreitier.sunspecexporter.sunspec;

/**
 * @author tho[at]dreitier[dot]com
 */
public class SunSpecException extends Exception{
    public SunSpecException(String message) {
        super(message);
    }

    public SunSpecException(String message, Throwable cause) {
        super(message, cause);
    }
}
