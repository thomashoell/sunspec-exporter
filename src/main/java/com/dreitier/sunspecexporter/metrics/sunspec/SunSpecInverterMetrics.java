package com.dreitier.sunspecexporter.metrics.sunspec;

import com.dreitier.sunspecexporter.sunspec.InverterIntegerMapFacade;
import com.dreitier.sunspecexporter.sunspec.SunSpecDevice;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.dreitier.sunspecexporter.metrics.Constants.*;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
@RequiredArgsConstructor
public class SunSpecInverterMetrics implements SunSpecMetrics {
    @NonNull
    private final MeterRegistry registry;
    @NonNull
    private final SunSpecDevice device;

    private List<Gauge> inverterMetrics = new ArrayList<>();

    private final AtomicDouble currentTotalValue = new AtomicDouble(0);
    private final AtomicDouble currentPhaseAValue = new AtomicDouble(0);
    private final AtomicDouble currentPhaseBValue = new AtomicDouble(0);
    private final AtomicDouble currentPhaseCValue = new AtomicDouble(0);
    private final AtomicDouble voltagePhaseABValue = new AtomicDouble(0);
    private final AtomicDouble voltagePhaseBCValue = new AtomicDouble(0);
    private final AtomicDouble voltagePhaseCAValue = new AtomicDouble(0);
    private final AtomicDouble voltagePhaseANValue = new AtomicDouble(0);
    private final AtomicDouble voltagePhaseBNValue = new AtomicDouble(0);
    private final AtomicDouble voltagePhaseCNValue = new AtomicDouble(0);
    private final AtomicDouble ACPowerValue = new AtomicDouble(0);
    private final AtomicDouble ACFrequencyValue = new AtomicDouble(0);
    private final AtomicDouble electricalPowerProducedValue = new AtomicDouble(0);
    private final AtomicDouble temperatureCabinet = new AtomicDouble(0);
    private final AtomicDouble temperatureHeatSink = new AtomicDouble(0);
    private final AtomicDouble temperatureOther = new AtomicDouble(0);
    private final AtomicDouble temperatureTransformer = new AtomicDouble(0);


    @Override
    public void refresh() {
        if (this.inverterMetrics.isEmpty()) {
            register();
        }

        try {
            InverterIntegerMapFacade inverterModel = device.getInverterIntegerMapModel();
            this.currentTotalValue.set(inverterModel.getAmps());
            this.currentPhaseAValue.set(inverterModel.getAmpsPhaseA());
            this.currentPhaseBValue.set(inverterModel.getAmpsPhaseB());
            this.currentPhaseCValue.set(inverterModel.getAmpsPhaseC());
            this.voltagePhaseABValue.set(inverterModel.getPhaseVoltageAB());
            this.voltagePhaseBCValue.set(inverterModel.getPhaseVoltageBC());
            this.voltagePhaseCAValue.set(inverterModel.getPhaseVoltageCA());
            this.voltagePhaseANValue.set(inverterModel.getPhaseVoltageAN());
            this.voltagePhaseBNValue.set(inverterModel.getPhaseVoltageBN());
            this.voltagePhaseCNValue.set(inverterModel.getPhaseVoltageCN());
            this.ACPowerValue.set(inverterModel.getWatts());
            this.ACFrequencyValue.set(inverterModel.getHz());
            this.electricalPowerProducedValue.set(inverterModel.getWattHours());
            this.temperatureCabinet.set(Optional.ofNullable(inverterModel.getCabinetTemperature()).orElse(0d));
            this.temperatureHeatSink.set(Optional.ofNullable(inverterModel.getHeatSinkTemperature()).orElse(0d));
            this.temperatureOther.set(Optional.ofNullable(inverterModel.getOtherTemperature()).orElse(0d));
            this.temperatureTransformer.set(Optional.ofNullable(inverterModel.getTransformerTemperature()).orElse(0d));

        } catch (Exception e) {
            log.error("failed to fetch metrics from device", e);
            drop();
        }
    }

    private void register() {
        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_CURRENT_OUTPUT, currentTotalValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_CURRENT_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_COMBINED
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_CURRENT_OUTPUT, currentPhaseAValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_CURRENT_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_A
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_CURRENT_OUTPUT, currentPhaseBValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_CURRENT_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_B
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_CURRENT_OUTPUT, currentPhaseCValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_CURRENT_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_C
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_VOLTAGE_OUTPUT, voltagePhaseABValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_VOLTAGE_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_AB
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_VOLTAGE_OUTPUT, voltagePhaseBCValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_VOLTAGE_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_BC
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_VOLTAGE_OUTPUT, voltagePhaseCAValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_VOLTAGE_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_CA
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_VOLTAGE_OUTPUT, voltagePhaseANValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_VOLTAGE_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_AN
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_VOLTAGE_OUTPUT, voltagePhaseBNValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_VOLTAGE_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_BN
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_VOLTAGE_OUTPUT, voltagePhaseCNValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_VOLTAGE_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC,
                                LABEL_NAME_PHASE, LABEL_VALUE_PHASE_CN
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_POWER_OUTPUT, ACPowerValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_POWER_OUTPUT)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_FREQUENCY, ACFrequencyValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_FREQUENCY)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_POWER_PRODUCED, electricalPowerProducedValue, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_POWER_PRODUCED)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_TYPE, LABEL_VALUE_AC
                        )
                        .register(registry)
        );
        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_TEMPERATURE, temperatureCabinet, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_TEMPERATURE)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_UNIT, LABEL_VALUE_UNIT_CABINET
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_TEMPERATURE, temperatureHeatSink, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_TEMPERATURE)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_UNIT, LABEL_VALUE_UNIT_HEAT_SINK
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_TEMPERATURE, temperatureOther, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_TEMPERATURE)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_UNIT, LABEL_VALUE_UNIT_OTHER
                        )
                        .register(registry)
        );

        this.inverterMetrics.add(
                Gauge
                        .builder(METRIC_NAME_TEMPERATURE, temperatureTransformer, AtomicDouble::doubleValue)
                        .description(METRIC_DESC_TEMPERATURE)
                        .tags(
                                LABEL_NAME_HOST, device.getDevice().getHost(),
                                LABEL_NAME_UNIT, LABEL_VALUE_UNIT_TRANSFORMER
                        )
                        .register(registry)
        );
    }

    @Override
    public void drop() {
        log.debug("dropping inverter metrics");
        inverterMetrics.forEach(registry::remove);
        this.inverterMetrics = new ArrayList<>();
    }
}
