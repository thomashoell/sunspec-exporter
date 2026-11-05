package com.dreitier.sunspecexporter.metrics;

import com.dreitier.sunspecexporter.sunspec.SunSpecDeviceRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MetricsConfiguration {

    /**
     * Registers the collector in the Prometheus registry behind {@code /actuator/prometheus}, next to the JVM and
     * HTTP metrics of Micrometer.
     */
    @Bean
    SunSpecCollector sunSpecCollector(SunSpecDeviceRegistry registry, PrometheusRegistry prometheusRegistry) {
        var collector = new SunSpecCollector(registry.devices());
        prometheusRegistry.register(collector);
        return collector;
    }
}
