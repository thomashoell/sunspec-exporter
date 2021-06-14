package com.dreitier.sunspecexporter.metrics.sunspec;

import com.dreitier.sunspecexporter.sunspec.InverterIntegerMapFacade;
import com.dreitier.sunspecexporter.sunspec.SunSpecDevice;
import com.dreitier.sunspecexporter.sunspec.SunSpecException;
import com.ghgande.j2mod.modbus.ModbusException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.energy.sunspec.MissingMandatoryFieldException;
import nl.basjes.energy.sunspec.ParseSunSpec;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
public class SunSpecCommonMetrics implements SunSpecMetrics {
    @NonNull
    private final MeterRegistry registry;
    @NonNull
    private final SunSpecDevice device;

    private Map<String, Object> modelMap;
    private String operatingState;

    private Gauge infoMetricsGauge;
    private final AtomicInteger infoMetricValue = new AtomicInteger(0);
    private final String infoMetricName = "sunspec_info";
    private final String infoMetricDesc = "Sunspec device info";

    private final Gauge statusMetricsGauge;
    private final AtomicInteger statusMetricValue = new AtomicInteger(0);
    private final String statusMetricName = "sunspec_up";
    private final String statusMetricDesc = "Status of sunspec device ";

    public SunSpecCommonMetrics(@NonNull final MeterRegistry registry, @NonNull final SunSpecDevice device) {
        this.registry = registry;
        this.device = device;
        this.statusMetricsGauge = Gauge
                .builder(statusMetricName, statusMetricValue, AtomicInteger::doubleValue)
                .description(statusMetricDesc)
                .tags("host", device.getDevice().getHost())
                .register(registry);
    }


    @Override
    public void refresh() throws SunSpecException {
        try {
            ParseSunSpec.Model_1 commonModel = device.getCommonModel();
            InverterIntegerMapFacade inverterModel = device.getInverterIntegerMapModel();
            if (null == modelMap || null == operatingState) {

                this.modelMap = commonModel.toHashMap();
                this.operatingState = inverterModel.getOperatingState();
                updateStatusMetric(commonModel, inverterModel);
            } else {
                final Map<String, Object> commonModelHashMap = commonModel.toHashMap();
                final String inverterState = inverterModel.getOperatingState();

                if (!this.modelMap.equals(commonModelHashMap) || !this.operatingState.equals(inverterState)) {
                    drop();
                    updateStatusMetric(commonModel, inverterModel);
                }
            }
            this.statusMetricValue.set(1);
        } catch (Exception e) {
            log.error("failed to fetch metrics from device", e);
            this.statusMetricValue.set(0);
            throw new SunSpecException("failed to fetch metrics from device", e);
        }
    }

    @Override
    public void drop() {
        if (null != infoMetricsGauge) {
            log.debug("dropping metric" + infoMetricName);
            this.registry.remove(infoMetricsGauge);
        }
    }

    private void updateStatusMetric(@NonNull final ParseSunSpec.Model_1 commonModel,
                                    @NonNull final InverterIntegerMapFacade inverterModel)
            throws MissingMandatoryFieldException, SunSpecException, ModbusException {

        this.infoMetricsGauge = Gauge
                .builder(infoMetricName, infoMetricValue, AtomicInteger::doubleValue)
                .description(infoMetricDesc)
                .tags(
                        "manufacturer", commonModel.getManufacturer(),
                        "model", commonModel.getModel(),
                        "serial", commonModel.getSerialNumber(),
                        "version", commonModel.getVersion(),
                        "mode", inverterModel.getOperatingState(),
                        "host", device.getDevice().getHost()
                )
                .register(registry);
    }
}
