package com.dreitier.sunspecexporter.sunspec;

import java.math.BigDecimal;

/**
 * Decodes SunSpec points from the raw registers of a model (big endian, offsets in registers).
 * <p>
 * Every data type has a "not implemented" value; points with that value, or whose scale factor is not implemented,
 * are returned as {@code null}. (The parsers of modbus-energy-parser return 0 for not implemented mandatory points,
 * which would e.g. report 0 V for phase B of a single phase inverter.)
 */
final class Registers {
    static final int NI_UINT16 = 0xFFFF;
    static final int NI_INT16 = 0x8000;
    static final long NI_ACC32 = 0;

    private Registers() {
    }

    enum Type {
        UINT16, INT16, ACC32
    }

    static int uint16(byte[] data, int offset) {
        return ((data[2 * offset] & 0xFF) << 8) | (data[2 * offset + 1] & 0xFF);
    }

    static long uint32(byte[] data, int offset) {
        return ((long) uint16(data, offset) << 16) | uint16(data, offset + 1);
    }

    /**
     * @return the raw value, {@code null} if not implemented
     */
    static Long raw(byte[] data, int offset, Type type) {
        if (2 * (offset + (type == Type.ACC32 ? 2 : 1)) > data.length) {
            return null;
        }
        return switch (type) {
            case UINT16 -> {
                int v = uint16(data, offset);
                yield v == NI_UINT16 ? null : (long) v;
            }
            case INT16 -> {
                int v = uint16(data, offset);
                yield v == NI_INT16 ? null : (long) (short) v;
            }
            case ACC32 -> {
                long v = uint32(data, offset);
                yield v == NI_ACC32 ? null : v;
            }
        };
    }

    /**
     * @return {@code value * 10^scaleFactor}, {@code null} if the value or the scale factor is not implemented
     */
    static Double scaled(byte[] data, int offset, Type type, int scaleFactorOffset) {
        Long value = raw(data, offset, type);
        Long scaleFactor = raw(data, scaleFactorOffset, Type.INT16);
        if (value == null || scaleFactor == null || scaleFactor < -10 || scaleFactor > 10) {
            return null;
        }
        // BigDecimal: 2301 * 10^-1 is 230.1, not 230.10000000000002
        return BigDecimal.valueOf(value).scaleByPowerOfTen(scaleFactor.intValue()).doubleValue();
    }
}
