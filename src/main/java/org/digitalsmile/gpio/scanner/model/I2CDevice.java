package org.digitalsmile.gpio.scanner.model;

import org.digitalsmile.gpio.i2c.attributes.I2CFunctionality;

import java.util.List;
import java.util.Map;

public record I2CDevice(String path, Map<I2CFunctionality, Boolean> functionalities, List<I2CAddress> addresses) {
}
