package com.dreitier.sunspecexporter.modbus.configuration;

import com.dreitier.sunspecexporter.configuration.utility.YamlPropertySourceFactory;
import com.dreitier.sunspecexporter.modbus.ModbusDevice;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * @author tho[at]dreitier[dot]com
 */
@PropertySource(value = "classpath:inverter.yaml", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class)
@PropertySource(value = "file:${SUNSPEC_EXPORTER_CONFIG_FILE}", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class)
@Configuration
@ConfigurationProperties(prefix = "modbus")
@Data
public class ModbusConfiguration {
    private List<ModbusDevice> devices;
}
