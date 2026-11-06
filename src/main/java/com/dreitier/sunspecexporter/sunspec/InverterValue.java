package com.dreitier.sunspecexporter.sunspec;

import static com.dreitier.sunspecexporter.sunspec.Registers.Type.ACC32;
import static com.dreitier.sunspecexporter.sunspec.Registers.Type.INT16;
import static com.dreitier.sunspecexporter.sunspec.Registers.Type.UINT16;

/**
 * Values of the SunSpec inverter models 101 (single phase), 102 (split phase) and 103 (three phase) and the metric
 * each value is exposed as.
 * <p>
 * The three models share the same register layout (offsets below are relative to the first register after ID and
 * length). Points a device does not implement (e.g. phase B and C of a single phase inverter) are not exposed.
 */
public enum InverterValue {
    AC_CURRENT_TOTAL(Metric.AC_CURRENT, "phase", "total", 0, UINT16, 4),
    AC_CURRENT_A(Metric.AC_CURRENT, "phase", "a", 1, UINT16, 4),
    AC_CURRENT_B(Metric.AC_CURRENT, "phase", "b", 2, UINT16, 4),
    AC_CURRENT_C(Metric.AC_CURRENT, "phase", "c", 3, UINT16, 4),

    AC_VOLTAGE_AB(Metric.AC_VOLTAGE, "phase", "ab", 5, UINT16, 11),
    AC_VOLTAGE_BC(Metric.AC_VOLTAGE, "phase", "bc", 6, UINT16, 11),
    AC_VOLTAGE_CA(Metric.AC_VOLTAGE, "phase", "ca", 7, UINT16, 11),
    AC_VOLTAGE_AN(Metric.AC_VOLTAGE, "phase", "an", 8, UINT16, 11),
    AC_VOLTAGE_BN(Metric.AC_VOLTAGE, "phase", "bn", 9, UINT16, 11),
    AC_VOLTAGE_CN(Metric.AC_VOLTAGE, "phase", "cn", 10, UINT16, 11),

    AC_POWER(Metric.AC_POWER, 12, INT16, 13),
    AC_FREQUENCY(Metric.AC_FREQUENCY, 14, UINT16, 15),
    AC_APPARENT_POWER(Metric.AC_APPARENT_POWER, 16, INT16, 17),
    AC_REACTIVE_POWER(Metric.AC_REACTIVE_POWER, 18, INT16, 19),
    AC_ENERGY(Metric.AC_ENERGY, 22, ACC32, 24),

    DC_CURRENT(Metric.DC_CURRENT, 25, UINT16, 26),
    DC_VOLTAGE(Metric.DC_VOLTAGE, 27, UINT16, 28),
    DC_POWER(Metric.DC_POWER, 29, INT16, 30),

    TEMPERATURE_CABINET(Metric.TEMPERATURE, "sensor", "cabinet", 31, INT16, 35),
    TEMPERATURE_HEAT_SINK(Metric.TEMPERATURE, "sensor", "heat_sink", 32, INT16, 35),
    TEMPERATURE_TRANSFORMER(Metric.TEMPERATURE, "sensor", "transformer", 33, INT16, 35),
    TEMPERATURE_OTHER(Metric.TEMPERATURE, "sensor", "other", 34, INT16, 35);

    /** Prometheus metric families of the inverter values. */
    public enum Metric {
        AC_CURRENT("sunspec_ac_current_amperes", "AC current of the inverter output", false),
        AC_VOLTAGE("sunspec_ac_voltage_volts", "AC voltage of the inverter output", false),
        AC_POWER("sunspec_ac_power_watts", "AC (active) power of the inverter output", false),
        AC_APPARENT_POWER("sunspec_ac_apparent_power_voltamperes", "AC apparent power of the inverter output", false),
        AC_REACTIVE_POWER("sunspec_ac_reactive_power_voltamperes_reactive",
                "AC reactive power of the inverter output", false),
        AC_FREQUENCY("sunspec_ac_frequency_hertz", "AC grid frequency", false),
        AC_ENERGY("sunspec_ac_energy_watthours", "AC energy produced over the lifetime of the inverter", true),
        DC_CURRENT("sunspec_dc_current_amperes", "DC current of the inverter input (all inputs)", false),
        DC_VOLTAGE("sunspec_dc_voltage_volts", "DC voltage of the inverter input", false),
        DC_POWER("sunspec_dc_power_watts", "DC power of the inverter input (all inputs)", false),
        TEMPERATURE("sunspec_temperature_celsius", "Temperature measured by the inverter", false);

        private final String metricName;
        private final String help;
        private final boolean counter;

        Metric(String metricName, String help, boolean counter) {
            this.metricName = metricName;
            this.help = help;
            this.counter = counter;
        }

        /** Name without the {@code _total} suffix that the exposition format adds to counters. */
        public String metricName() {
            return metricName;
        }

        public String help() {
            return help;
        }

        public boolean counter() {
            return counter;
        }
    }

    /** Register offset of the operating state (St, enum16) in the inverter models. */
    static final int OFFSET_OPERATING_STATE = 36;

    private final Metric metric;
    private final String labelName;
    private final String labelValue;
    private final int offset;
    private final Registers.Type type;
    private final int scaleFactorOffset;

    InverterValue(Metric metric, int offset, Registers.Type type, int scaleFactorOffset) {
        this(metric, null, null, offset, type, scaleFactorOffset);
    }

    InverterValue(Metric metric, String labelName, String labelValue, int offset, Registers.Type type,
                  int scaleFactorOffset) {
        this.metric = metric;
        this.labelName = labelName;
        this.labelValue = labelValue;
        this.offset = offset;
        this.type = type;
        this.scaleFactorOffset = scaleFactorOffset;
    }

    public Metric metric() {
        return metric;
    }

    /** Name of the label that distinguishes values of the same metric, {@code null} if there is none. */
    public String labelName() {
        return labelName;
    }

    public String labelValue() {
        return labelValue;
    }

    /**
     * @param data raw registers of model 101, 102 or 103 (without ID and length)
     * @return the scaled value, {@code null} if the device doesn't implement it
     */
    Double parse(byte[] data) {
        return Registers.scaled(data, offset, type, scaleFactorOffset);
    }
}
