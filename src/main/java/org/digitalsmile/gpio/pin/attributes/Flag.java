package org.digitalsmile.gpio.pin.attributes;

public enum Flag {
    KERNEL(1),  // 1
    IS_OUT(1 << 1), // 2
    ACTIVE_LOW(1 << 2), // 4
    OPEN_DRAIN(1 << 3), // 8
    OPEN_SOURCE(1 << 4), // 16
    BIAS_PULL_UP(1 << 5), // 32
    BIAS_PULL_DOWN(1 << 6), // 64
    BIAS_DISABLE(1 << 7); // 128;


    private final int value;
    Flag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
