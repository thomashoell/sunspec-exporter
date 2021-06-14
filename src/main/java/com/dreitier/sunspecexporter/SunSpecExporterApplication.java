package com.dreitier.sunspecexporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SunSpecExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunSpecExporterApplication.class, args);
    }

}
