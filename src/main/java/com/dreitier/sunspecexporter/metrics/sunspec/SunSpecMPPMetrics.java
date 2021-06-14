package com.dreitier.sunspecexporter.metrics.sunspec;

import com.dreitier.sunspecexporter.sunspec.SunSpecDevice;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.energy.sunspec.ParseSunSpec;

import java.util.*;

import static com.dreitier.sunspecexporter.metrics.Constants.*;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
@RequiredArgsConstructor
public class SunSpecMPPMetrics implements SunSpecMetrics {
    @NonNull
    private final MeterRegistry registry;
    @NonNull
    private final SunSpecDevice device;

    private List<Gauge> MPPMetrics = new ArrayList<>();
    private Map<Integer, AtomicDouble> inputCurrent = new HashMap<>();
    private Map<Integer, AtomicDouble> inputVoltage = new HashMap<>();
    private Map<Integer, AtomicDouble> inputPower = new HashMap<>();

    private int numberOfInputs = 0;

    @Override
    public void refresh() {
        try {
            ParseSunSpec.Model_160 MPPModel = device.getMPPTInverterExtensionModel();
            final int numOfInputs = MPPModel.getNumberofModules();

            if (this.MPPMetrics.isEmpty() || this.numberOfInputs != numOfInputs || this.MPPMetrics.size() != numOfInputs) {
                register(MPPModel, numOfInputs);
            }

            for (int i = 0; i < numOfInputs; i++){
                this.inputCurrent.get(i).set(Optional.ofNullable(MPPModel.getRepeatingDCCurrent(i)).orElse(0d));
                this.inputVoltage.get(i).set(Optional.ofNullable(MPPModel.getRepeatingDCVoltage(i)).orElse(0d));
                this.inputPower.get(i).set(Optional.ofNullable(MPPModel.getRepeatingDCPower(i)).orElse(0d));
            }

        } catch (Exception e) {
            log.error("failed to fetch metrics from device", e);
            drop();
        }
    }

    private void register(final ParseSunSpec.Model_160 MPPModel, int numOfInputs) {
        drop();
        for (int i = 0; i < numOfInputs; i++){
            final AtomicDouble inputCurrent = new AtomicDouble(0d);
            final AtomicDouble inputVoltage = new AtomicDouble(0d);
            final AtomicDouble inputPower = new AtomicDouble(0d);
            this.MPPMetrics.add(
                    Gauge
                            .builder(METRIC_NAME_CURRENT_INPUT, inputCurrent, AtomicDouble::doubleValue)
                            .description(METRIC_DESC_CURRENT_INPUT)
                            .tags(
                                    LABEL_NAME_HOST, device.getDevice().getHost(),
                                    LABEL_NAME_TYPE, LABEL_VALUE_DC,
                                    LABEL_NAME_INPUT, String.valueOf(i)
                            )
                            .register(registry)
            );
            this.MPPMetrics.add(
                    Gauge
                            .builder(METRIC_NAME_VOLTAGE_INPUT, inputVoltage, AtomicDouble::doubleValue)
                            .description(METRIC_DESC_VOLTAGE_INPUT)
                            .tags(
                                    LABEL_NAME_HOST, device.getDevice().getHost(),
                                    LABEL_NAME_TYPE, LABEL_VALUE_DC,
                                    LABEL_NAME_INPUT, String.valueOf(i)
                            )
                            .register(registry)
            );
            this.MPPMetrics.add(
                    Gauge
                            .builder(METRIC_NAME_POWER_INPUT, inputPower, AtomicDouble::doubleValue)
                            .description(METRIC_DESC_POWER_INPUT)
                            .tags(
                                    LABEL_NAME_HOST, device.getDevice().getHost(),
                                    LABEL_NAME_TYPE, LABEL_VALUE_DC,
                                    LABEL_NAME_INPUT, String.valueOf(i)
                            )
                            .register(registry)
            );
            this.inputCurrent.put(i, inputCurrent);
            this.inputVoltage.put(i, inputVoltage);
            this.inputPower.put(i, inputPower);
        }
        this.numberOfInputs = numOfInputs;
    }

    @Override
    public void drop() {
        log.debug("dropping MPP metrics");
        MPPMetrics.forEach(registry::remove);
        this.MPPMetrics = new ArrayList<>();
        this.inputCurrent = new HashMap<>();
        this.inputVoltage = new HashMap<>();
        this.inputPower = new HashMap<>();
    }
}