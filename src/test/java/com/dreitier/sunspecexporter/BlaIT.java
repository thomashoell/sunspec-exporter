package com.dreitier.sunspecexporter;

import com.dreitier.sunspecexporter.sunspec.InverterIntegerMapFacade;
import com.dreitier.sunspecexporter.sunspec.SunSpecException;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import nl.basjes.energy.sunspec.MissingMandatoryFieldException;
import nl.basjes.energy.sunspec.ParseSunSpec;
import nl.basjes.energy.sunspec.SunSpecFetcher;
import nl.basjes.energy.sunspec.SunSpecModbusDataReader;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author tho[at]dreitier[dot]com
 */
public class BlaIT {

    @Test
    void name2() throws ModbusException, SunSpecException {
        SunSpecModbusDataReader dataReader = new SunSpecModbusDataReader(new ModbusTCPMaster("192.168.241.100"));
        SunSpecFetcher fetcher = new SunSpecFetcher(dataReader);
        InverterIntegerMapFacade facade = new InverterIntegerMapFacade(fetcher.useModel(103).model_103);
//        Double r = facade.getAmps();
//        String s = facade.getOperatingState();
        Map m = facade.toHashMap();
        System.out.println();
    }

    @Test
    void name() {
        try (SunSpecModbusDataReader dataReader = new SunSpecModbusDataReader(new ModbusTCPMaster("192.168.241.100"))) {
            Map<Integer, SunSpecModbusDataReader.ModelLocation> locations = dataReader.getModelLocations();
            SunSpecFetcher fetcher = new SunSpecFetcher(dataReader);
            ParseSunSpec.Model_1 commonModel = fetcher.useModel(1).model_1;
            System.out.printf("%s - %s - %s - %s - %s%n", commonModel.getManufacturer(), commonModel.getModel(), commonModel.getOptions(), commonModel.getVersion(), commonModel.getSerialNumber());

            ParseSunSpec.Model_103 inverterIntegerMap = fetcher.useModel(103).model_103;
            Double cabinetTemperature = inverterIntegerMap.getCabinetTemperature();
            Double heatSinkTemp = inverterIntegerMap.getHeatSinkTemperature();
            Double transformerTemp = inverterIntegerMap.getTransformerTemperature();
            Double otherTemp = inverterIntegerMap.getOtherTemperature();
            String desc = inverterIntegerMap.getDescription();
            String label = inverterIntegerMap.getLabel();
            String notes = inverterIntegerMap.getNotes();
            Double amps = inverterIntegerMap.getAmps();
            Double frequency = inverterIntegerMap.getHz();
            ParseSunSpec.Model_103.OperatingState operatingState = inverterIntegerMap.getOperatingState();
            Double DCAmps = inverterIntegerMap.getDCAmps();
            Double p1Amps = inverterIntegerMap.getAmpsPhaseA();
            Double p2Amps = inverterIntegerMap.getAmpsPhaseB();
            Double p3Amps = inverterIntegerMap.getAmpsPhaseC();
            Double DCVoltage = inverterIntegerMap.getDCVoltage();
            Double watts = inverterIntegerMap.getWatts();
            Double wattHours = inverterIntegerMap.getWattHours();
            Double voltageP1 = inverterIntegerMap.getPhaseVoltageAN();
            Double voltageP2 = inverterIntegerMap.getPhaseVoltageBN();
            Double voltageP3 = inverterIntegerMap.getPhaseVoltageCN();
            Double tempHeatSink = inverterIntegerMap.getHeatSinkTemperature();
            Map<String, Object> map = inverterIntegerMap.toHashMap();



            Map<String, Object> m = fetcher.useAllModels().toHashMap();

            m.entrySet().stream()
                    .filter(stringObjectEntry ->stringObjectEntry.getValue().getClass().isAssignableFrom(Double.class))
                    .forEach(stringObjectEntry -> System.out.printf("%s:\t\t %5.2f\n", stringObjectEntry.getKey(), stringObjectEntry.getValue()));

            Map<String, Object> m160 = fetcher.model_160.toHashMap();
            System.out.println();

        } catch (ModbusException | MissingMandatoryFieldException e) {
            e.printStackTrace();
        }
    }
}