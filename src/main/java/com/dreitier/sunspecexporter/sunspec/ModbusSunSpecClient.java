package com.dreitier.sunspecexporter.sunspec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import nl.basjes.energy.sunspec.MissingMandatoryFieldException;
import nl.basjes.energy.sunspec.ParseSunSpec;
import nl.basjes.energy.sunspec.SunSpecModbusDataReader;
import nl.basjes.energy.sunspec.SunSpecModbusDataReader.ModelLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a SunSpec device via Modbus TCP.
 * <p>
 * On connect the SunSpec model locations and the common model (1) are read once. Each {@link #read()} then reads the
 * inverter model (101/102/103) and, if present, the MPPT extension model (160) with one block read per model.
 * Other models the device implements are never read. Any error closes the connection, the next read reconnects;
 * this covers inverters that switch off their Modbus interface at night.
 */
public final class ModbusSunSpecClient implements SunSpecClient {
    private static final Logger log = LoggerFactory.getLogger(ModbusSunSpecClient.class);

    static final int MODEL_COMMON = 1;
    /** Inverter models in order of preference: three phase, split phase, single phase. */
    static final int[] MODELS_INVERTER = {103, 102, 101};
    static final int MODEL_MPPT = 160;

    private static final ParseSunSpec.Model_1 COMMON_PARSER = new ParseSunSpec.Model_1();

    /** Layout of the multiple MPPT inverter extension model (160), offsets in registers after ID and length. */
    private static final int MPPT_SF_CURRENT = 0;
    private static final int MPPT_SF_VOLTAGE = 1;
    private static final int MPPT_SF_POWER = 2;
    private static final int MPPT_SF_ENERGY = 3;
    private static final int MPPT_COUNT = 6;
    private static final int MPPT_FIXED_LENGTH = 8;
    private static final int MPPT_INPUT_LENGTH = 20;
    private static final int MPPT_INPUT_LABEL = 1;
    private static final int MPPT_INPUT_LABEL_LENGTH = 8;
    private static final int MPPT_INPUT_CURRENT = 9;
    private static final int MPPT_INPUT_VOLTAGE = 10;
    private static final int MPPT_INPUT_POWER = 11;
    private static final int MPPT_INPUT_ENERGY = 12;

    private final ModbusProperties.Device device;
    private final int timeoutMillis;

    private Session session;

    public ModbusSunSpecClient(ModbusProperties.Device device, Duration timeout) {
        this.device = device;
        this.timeoutMillis = Math.toIntExact(timeout.toMillis());
    }

    @Override
    public synchronized DeviceReading read() throws Exception {
        if (session == null) {
            session = connect();
        }
        try {
            return session.read();
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    public synchronized void close() {
        if (session != null) {
            session.reader.close();
            session = null;
        }
    }

    private Session connect() throws ModbusException, SunSpecException {
        log.debug("connecting to {}:{} (unit {})", device.host(), device.port(), device.unitId());
        var master = new ModbusTCPMaster(device.host(), device.port(), timeoutMillis, false, false);
        SunSpecModbusDataReader reader;
        try {
            // connects and discovers the SunSpec models of the device
            reader = new SunSpecModbusDataReader(master, SunSpecModbusDataReader.SUNSPEC_STANDARD_STARTBASE,
                    device.unitId());
        } catch (ModbusException | RuntimeException e) {
            master.disconnect();
            throw e;
        }

        try {
            Map<Integer, ModelLocation> locations = reader.getModelLocations();
            ModelLocation common = locations.get(MODEL_COMMON);
            if (common == null) {
                throw new SunSpecException("device doesn't implement the SunSpec common model (1)");
            }
            ModelLocation inverter = null;
            for (int model : MODELS_INVERTER) {
                inverter = locations.get(model);
                if (inverter != null) {
                    break;
                }
            }
            if (inverter == null) {
                throw new SunSpecException("device doesn't implement a SunSpec inverter model (101, 102 or 103), "
                        + "implemented models: " + locations.keySet());
            }
            ModelLocation mppt = locations.get(MODEL_MPPT);

            DeviceInfo info = readInfo(reader.getRawModel(common), inverter.id);
            log.info("connected to {} {} (serial {}, firmware {}) at {}:{}, SunSpec models {}", info.manufacturer(),
                    info.model(), info.serialNumber(), info.version(), device.host(), device.port(),
                    locations.keySet());
            return new Session(reader, info, inverter, mppt);
        } catch (ModbusException | SunSpecException | RuntimeException e) {
            reader.close();
            throw e;
        }
    }

    static DeviceInfo readInfo(byte[] data, int inverterModel) {
        return new DeviceInfo(
                text(() -> COMMON_PARSER.getManufacturer(data)),
                text(() -> COMMON_PARSER.getModel(data)),
                text(() -> COMMON_PARSER.getVersion(data)),
                text(() -> COMMON_PARSER.getSerialNumber(data)),
                inverterModel);
    }

    static Map<InverterValue, Double> readInverter(byte[] data) {
        var values = new EnumMap<InverterValue, Double>(InverterValue.class);
        for (InverterValue value : InverterValue.values()) {
            Double v = value.parse(data);
            if (v != null) {
                values.put(value, v);
            }
        }
        return values;
    }

    static String readOperatingState(byte[] data) {
        Long value = Registers.raw(data, InverterValue.OFFSET_OPERATING_STATE, Registers.Type.UINT16);
        OperatingState state = value == null ? null : OperatingState.of(value.intValue());
        return state == null ? null : state.name();
    }

    static List<MpptInput> readMppt(byte[] data) {
        Long count = Registers.raw(data, MPPT_COUNT, Registers.Type.UINT16);
        if (count == null || count <= 0) {
            return List.of();
        }
        // a device may announce more inputs than the model contains
        int inputs = (int) Math.min(count, (data.length / 2 - MPPT_FIXED_LENGTH) / MPPT_INPUT_LENGTH);
        var result = new ArrayList<MpptInput>(inputs);
        for (int i = 0; i < inputs; i++) {
            int base = MPPT_FIXED_LENGTH + i * MPPT_INPUT_LENGTH;
            String label = new String(data, 2 * (base + MPPT_INPUT_LABEL), 2 * MPPT_INPUT_LABEL_LENGTH,
                    StandardCharsets.US_ASCII);
            result.add(new MpptInput(
                    i,
                    clean(label),
                    Registers.scaled(data, base + MPPT_INPUT_CURRENT, Registers.Type.UINT16, MPPT_SF_CURRENT),
                    Registers.scaled(data, base + MPPT_INPUT_VOLTAGE, Registers.Type.UINT16, MPPT_SF_VOLTAGE),
                    Registers.scaled(data, base + MPPT_INPUT_POWER, Registers.Type.UINT16, MPPT_SF_POWER),
                    Registers.scaled(data, base + MPPT_INPUT_ENERGY, Registers.Type.ACC32, MPPT_SF_ENERGY)));
        }
        return result;
    }

    @FunctionalInterface
    private interface TextField {
        String get() throws MissingMandatoryFieldException;
    }

    private static String text(TextField field) {
        try {
            return clean(field.get());
        } catch (MissingMandatoryFieldException e) {
            return "";
        }
    }

    /** SunSpec strings are padded with NUL characters. */
    private static String clean(String value) {
        return value == null ? "" : value.replace('\0', ' ').strip();
    }

    private record Session(SunSpecModbusDataReader reader, DeviceInfo info, ModelLocation inverter,
                           ModelLocation mppt) {

        DeviceReading read() throws ModbusException {
            byte[] inverterData = reader.getRawModel(inverter);
            List<MpptInput> inputs = mppt == null ? List.of() : readMppt(reader.getRawModel(mppt));
            return new DeviceReading(info, readInverter(inverterData), readOperatingState(inverterData), inputs);
        }
    }
}
