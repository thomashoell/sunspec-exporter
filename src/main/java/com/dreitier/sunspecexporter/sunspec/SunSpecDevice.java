package com.dreitier.sunspecexporter.sunspec;

import com.dreitier.sunspecexporter.modbus.ModbusDevice;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.energy.sunspec.ParseSunSpec;
import nl.basjes.energy.sunspec.SunSpecFetcher;
import nl.basjes.energy.sunspec.SunSpecModbusDataReader;

import java.util.HashSet;
import java.util.Set;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
@RequiredArgsConstructor
public class SunSpecDevice {
    @Getter
    private final ModbusDevice device;
    private SunSpecFetcher fetcher;
    private Set<Integer> supportedModels;

    private void connectModbusMaster() throws SunSpecException {
        if (null == this.fetcher) {
            try {
                final SunSpecModbusDataReader dataReader = new SunSpecModbusDataReader(
                        new ModbusTCPMaster(device.getHost(), device.getPort()));

                this.supportedModels = new HashSet<>(dataReader.getModelLocations().keySet());

                SunSpecFetcher f = new SunSpecFetcher(dataReader);
                f.useAllModels();
                this.fetcher = f;
            } catch (ModbusException e) {
                this.fetcher = null;
                log.error("failed to connect to Modbus master", e);
                throw new SunSpecException("failed to connect to Modbus master", e);
            }
        }
    }

    /**
     * Fetch data from device
     *
     * @throws SunSpecException
     */
    public void refresh() throws SunSpecException {
        try {
            connectModbusMaster();
            fetcher.refresh();
        } catch (ModbusException e) {
            log.error("failed to refresh data from Modbus master", e);
            throw new SunSpecException("failed to refresh data from Modbus master", e);
        }
    }

    private void checkModelSupport(@NonNull final Integer modelNumber) throws SunSpecException {
        connectModbusMaster();

        if (!this.supportedModels.contains(modelNumber)) {
            log.error("model {} not supported", modelNumber);
            throw new SunSpecException("model " + modelNumber + " not supported");
        }
    }

    public ParseSunSpec.Model_1 getCommonModel() throws SunSpecException {
        try {
            connectModbusMaster();
            return fetcher.model_1;
        } catch (SunSpecException e) {
            log.error("Failed to read common model table", e);
            throw new SunSpecException("Failed to read common model table", e);
        }
    }

    public ParseSunSpec.Model_11 getEthernetLinkLayerModel() throws SunSpecException {
        try {
            connectModbusMaster();
            checkModelSupport(11);
            return fetcher.model_11;
        } catch (SunSpecException e) {
            log.error("Failed to read ethernet link layer model table", e);
            throw new SunSpecException("Failed to read ethernet link layer model table", e);
        }
    }

    public ParseSunSpec.Model_12 getIPv4Model() throws SunSpecException {
        try {
            connectModbusMaster();
            checkModelSupport(12);
            return fetcher.model_12;
        } catch (SunSpecException e) {
            log.error("Failed to read IPv4 model table", e);
            throw new SunSpecException("Failed to read IPv4 model table", e);
        }
    }

    public InverterIntegerMapFacade getInverterIntegerMapModel() throws SunSpecException {
        try {
            connectModbusMaster();
            if (supportedModels.contains(101)) {
                return new InverterIntegerMapFacade(fetcher.model_101);
            } else if (supportedModels.contains(102)) {
                return new InverterIntegerMapFacade(fetcher.model_102);
            } else if (supportedModels.contains(103)) {
                return new InverterIntegerMapFacade(fetcher.model_103);
            } else {
                log.error("Device doesn't support inverter integer map model");
                throw new SunSpecException("Device doesn't support inverter integer map model");
            }
        } catch (SunSpecException e) {
            log.error("Failed to read IPv4 model table", e);
            throw new SunSpecException("Failed to read IPv4 model table", e);
        }
    }

    public ParseSunSpec.Model_160 getMPPTInverterExtensionModel() throws SunSpecException {
        try {
            connectModbusMaster();
            checkModelSupport(160);
            return fetcher.model_160;
        } catch (SunSpecException e) {
            log.error("Failed to read MPPT Inverter Extension Model table", e);
            throw new SunSpecException("Failed to read MPPT Inverter Extension Model table", e);
        }
    }
}
