package org.digitalsmile.gpio.scanner.model;

import org.digitalsmile.gpio.i2c.attributes.I2CStatus;

public record I2CAddress(int address, I2CStatus status) {
}
