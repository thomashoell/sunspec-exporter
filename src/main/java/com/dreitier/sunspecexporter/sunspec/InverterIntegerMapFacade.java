package com.dreitier.sunspecexporter.sunspec;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.energy.sunspec.ModelFetcher;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
@RequiredArgsConstructor
public class InverterIntegerMapFacade {
    private final ModelFetcher model;

    private Object reflectiveGetter(@NonNull final String name) throws Exception {
        Assert.hasText(name, "method name must be specified");
        Class<?> modelClass = model.getClass();
        Method method = modelClass.getDeclaredMethod(name);

        try {
            return method.invoke(model);
        } catch (Exception e) {
            log.error("failed to execute method with name " + name + " on object of type " + modelClass.getName());
            throw e;
        }
    }

    public Double getAmps() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getAmps");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getAmpsPhaseA() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getAmpsPhaseA");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getAmpsPhaseB() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getAmpsPhaseB");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getAmpsPhaseC() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getAmpsPhaseC");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getPhaseVoltageAB() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getPhaseVoltageAB");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getPhaseVoltageBC() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getPhaseVoltageBC");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getPhaseVoltageCA() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getPhaseVoltageCA");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getPhaseVoltageAN() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getPhaseVoltageAN");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getPhaseVoltageBN() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getPhaseVoltageBN");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getPhaseVoltageCN() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getPhaseVoltageCN");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getWatts() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getWatts");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getHz() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getHz");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getVA() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getVA");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getWattHours() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getWattHours");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getDCAmps() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getDCAmps");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getDCVoltage() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getDCVoltage");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getDCWatts() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getDCWatts");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getCabinetTemperature() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getCabinetTemperature");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getHeatSinkTemperature() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getHeatSinkTemperature");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getTransformerTemperature() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getTransformerTemperature");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Double getOtherTemperature() throws SunSpecException {
        try {
            return (Double) reflectiveGetter("getOtherTemperature");
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public String getOperatingState() throws SunSpecException {
        try {
            return reflectiveGetter("getOperatingState").toString();
        } catch (Exception e) {
            throw new SunSpecException("failed to fetch data from model", e);
        }
    }

    public Map<String, Object> toHashMap(){
        return model.toHashMap();
    }

}
