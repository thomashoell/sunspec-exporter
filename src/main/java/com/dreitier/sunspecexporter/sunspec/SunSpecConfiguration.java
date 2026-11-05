package com.dreitier.sunspecexporter.sunspec;

import java.time.Clock;

import com.dreitier.sunspecexporter.config.ModbusProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SunSpecConfiguration {

    @Bean
    SunSpecClientFactory sunSpecClientFactory(ModbusProperties properties) {
        return device -> new ModbusSunSpecClient(device, properties.timeout());
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
