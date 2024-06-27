package org.digitalsmile.gpio.scanner.model;

import java.util.List;

public record GPIODevice(String path, String name, String label, List<GPIOLine> lines) {
}
