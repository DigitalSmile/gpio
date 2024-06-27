package org.digitalsmile.gpio.scanner.model;

import org.digitalsmile.gpio.pin.attributes.PinFlag;

import java.util.List;

public record GPIOLine(int pin, String name, String consumer, List<PinFlag> flags) {
}
