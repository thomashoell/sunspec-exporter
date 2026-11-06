package com.dreitier.sunspecexporter.sunspec;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

/**
 * A Modbus TCP slave serving the holding registers of a SunSpec inverter (models 1, 101/102/103 and optionally 160),
 * built after the SunSpec model definitions. Values are given as raw register values plus scale factors.
 */
public final class SunSpecSimulator implements AutoCloseable {
    public static final int UNIT_ID = 126;
    private static final int BASE = 40000;

    /** Not implemented markers of the SunSpec data types. */
    public static final int NI_INT16 = 0x8000;
    public static final int NI_UINT16 = 0xFFFF;

    private final int port;
    private final List<Integer> registers;
    private ModbusSlave slave;

    private SunSpecSimulator(int port, List<Integer> registers) {
        this.port = port;
        this.registers = registers;
    }

    public int port() {
        return port;
    }

    /** Starts the simulator on a free port. */
    public static SunSpecSimulator start(Builder builder) throws Exception {
        var simulator = new SunSpecSimulator(freePort(), builder.registers());
        simulator.open();
        return simulator;
    }

    /** (Re-)opens the Modbus TCP listener. */
    public synchronized void open() throws ModbusException {
        var image = new SimpleProcessImage(UNIT_ID);
        for (int i = 0; i < BASE; i++) {
            image.addRegister(new SimpleRegister(0));
        }
        registers.forEach(r -> image.addRegister(new SimpleRegister(r & 0xFFFF)));
        slave = ModbusSlaveFactory.createTCPSlave(port, 5);
        slave.addProcessImage(UNIT_ID, image);
        slave.open();
    }

    @Override
    public void close() {
        stop();
    }

    /** Stops the listener, e.g. to simulate an inverter that switched off its Modbus interface. */
    public synchronized void stop() {
        if (slave != null) {
            ModbusSlaveFactory.close(slave);
            slave = null;
        }
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public static Builder threePhaseInverter() {
        return new Builder(103);
    }

    public static Builder singlePhaseInverter() {
        return new Builder(101);
    }

    /** Device that implements only the common model (e.g. a meter without inverter model). */
    public static Builder commonModelOnly() {
        return new Builder(0);
    }

    public static final class Builder {
        private final int inverterModel;
        private String manufacturer = "Fronius";
        private String model = "Symo 8.2-3-M";
        private String version = "0.3.30.2";
        private String serial = "12345678";
        private int operatingState = 4; // MPPT
        private int mpptInputs = 2;

        private Builder(int inverterModel) {
            this.inverterModel = inverterModel;
        }

        public Builder serial(String serial) {
            this.serial = serial;
            return this;
        }

        public Builder operatingState(int operatingState) {
            this.operatingState = operatingState;
            return this;
        }

        public Builder mpptInputs(int mpptInputs) {
            this.mpptInputs = mpptInputs;
            return this;
        }

        List<Integer> registers() {
            var r = new ArrayList<Integer>();
            r.add(0x5375); // "SunS"
            r.add(0x6E53);

            // model 1: common
            r.add(1);
            r.add(66);
            string(r, manufacturer, 16);
            string(r, model, 16);
            string(r, "", 8); // options
            string(r, version, 8);
            string(r, serial, 16);
            r.add(1); // device address
            r.add(0x8000); // pad

            if (inverterModel != 0) {
                boolean threePhase = inverterModel == 103;
                r.add(inverterModel);
                r.add(50);
                r.add(1234); // A
                r.add(threePhase ? 411 : 1234); // AphA
                r.add(threePhase ? 412 : NI_UINT16); // AphB
                r.add(threePhase ? 411 : NI_UINT16); // AphC
                r.add(-2); // A_SF
                r.add(threePhase ? 4005 : NI_UINT16); // PPVphAB
                r.add(threePhase ? 4010 : NI_UINT16); // PPVphBC
                r.add(threePhase ? 3998 : NI_UINT16); // PPVphCA
                r.add(2301); // PhVphA
                r.add(threePhase ? 2305 : NI_UINT16); // PhVphB
                r.add(threePhase ? 2299 : NI_UINT16); // PhVphC
                r.add(-1); // V_SF
                r.add(2840); // W
                r.add(0); // W_SF
                r.add(5001); // Hz
                r.add(-2); // Hz_SF
                r.add(2850); // VA
                r.add(0); // VA_SF
                r.add(NI_INT16); // VAr
                r.add(NI_INT16); // VAr_SF
                r.add(NI_INT16); // PF
                r.add(NI_INT16); // PF_SF
                r.add(0x0001); // WH (acc32): 0x0001E240 = 123456
                r.add(0xE240);
                r.add(1); // WH_SF -> 1234560 Wh
                r.add(NI_UINT16); // DCA
                r.add(NI_INT16); // DCA_SF
                r.add(NI_UINT16); // DCV
                r.add(NI_INT16); // DCV_SF
                r.add(2950); // DCW
                r.add(0); // DCW_SF
                r.add(NI_INT16); // TmpCab
                r.add(452); // TmpSnk
                r.add(NI_INT16); // TmpTrns
                r.add(NI_INT16); // TmpOt
                r.add(-1); // Tmp_SF
                r.add(operatingState); // St
                r.add(NI_UINT16); // StVnd
                for (int i = 0; i < 12; i++) {
                    r.add(0); // Evt1, Evt2, EvtVnd1-4 (bitfield32)
                }
            }

            if (mpptInputs > 0) {
                // model 160: multiple MPPT inverter extension
                r.add(160);
                r.add(8 + 20 * mpptInputs);
                r.add(-2); // DCA_SF
                r.add(-1); // DCV_SF
                r.add(0); // DCW_SF
                r.add(0); // DCWH_SF
                r.add(0); // Evt (bitfield32)
                r.add(0);
                r.add(mpptInputs); // N
                r.add(NI_UINT16); // TmsPer
                for (int i = 0; i < mpptInputs; i++) {
                    r.add(i + 1); // ID
                    string(r, "String " + (i + 1), 8); // IDStr
                    r.add(500 + i); // DCA -> 5.00 A
                    r.add(6000 + i); // DCV -> 600.0 V
                    r.add(1500 + i); // DCW
                    r.add(0); // DCWH (acc32)
                    r.add(10_000 + i);
                    r.add(0); // Tms (uint32)
                    r.add(0);
                    r.add(NI_INT16); // Tmp
                    r.add(4); // DCSt
                    r.add(0); // DCEvt (bitfield32)
                    r.add(0);
                }
            }

            r.add(0xFFFF); // end model
            r.add(0);
            return r;
        }

        private static void string(List<Integer> r, String value, int registers) {
            byte[] bytes = new byte[registers * 2];
            byte[] text = value.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(text, 0, bytes, 0, Math.min(text.length, bytes.length));
            for (int i = 0; i < registers; i++) {
                r.add(((bytes[2 * i] & 0xFF) << 8) | (bytes[2 * i + 1] & 0xFF));
            }
        }
    }
}
