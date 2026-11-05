package com.dreitier.sunspecexporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class SunSpecExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunSpecExporterApplication.class, args);
    }
}
