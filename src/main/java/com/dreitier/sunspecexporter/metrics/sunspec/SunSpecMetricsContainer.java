package com.dreitier.sunspecexporter.metrics.sunspec;

import com.dreitier.sunspecexporter.sunspec.SunSpecDevice;
import com.dreitier.sunspecexporter.sunspec.SunSpecException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
public class SunSpecMetricsContainer {
    @NonNull
    private final SunSpecDevice device;

    @NonNull
    private final List<SunSpecMetrics> metrics = new LinkedList<>();

    public SunSpecMetricsContainer(@NonNull final SunSpecDevice device, @NonNull final MeterRegistry registry) {
        this.device = device;
        this.metrics.add(new SunSpecCommonMetrics(registry, device));
        this.metrics.add(new SunSpecInverterMetrics(registry, device));
        this.metrics.add(new SunSpecMPPMetrics(registry, device));
    }

    public void refresh() {
        try {
            device.refresh();
            for (SunSpecMetrics metric : metrics) {
                metric.refresh();
            }
        } catch (SunSpecException e) {
            log.error("failed to refresh metrics");
            metrics.forEach(SunSpecMetrics::drop);
        }
    }
}
