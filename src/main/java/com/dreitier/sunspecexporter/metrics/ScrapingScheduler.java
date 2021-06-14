package com.dreitier.sunspecexporter.metrics;

import com.dreitier.sunspecexporter.metrics.sunspec.SunSpecMetricsContainer;
import com.dreitier.sunspecexporter.sunspec.SunSpecDeviceRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tho[at]dreitier[dot]com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingScheduler {
    private final SunSpecDeviceRegistry deviceRegistry;
    private final MeterRegistry prometheusRegistry;
    private List<SunSpecMetricsContainer> metricsContainers;

    @Scheduled(fixedRateString = "5000", initialDelayString = "0")
    public void schedulingTask() {
        if (log.isDebugEnabled()) {
            log.debug("scraping devices...");
        }
        this.metricsContainers.forEach(SunSpecMetricsContainer::refresh);
    }

    @PostConstruct
    private void init() {
        this.metricsContainers = deviceRegistry.getSunSpecDevices()
                .stream()
                .map(sunSpecDevice -> new SunSpecMetricsContainer(sunSpecDevice, prometheusRegistry))
                .collect(Collectors.toList());
    }
}
