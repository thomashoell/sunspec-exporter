package com.dreitier.sunspecexporter.modbus;

import lombok.Data;

/**
 * @author tho[at]dreitier[dot]com
 */
@Data
public class ModbusDevice {
    private String host;
    private Integer port = 502;
}
