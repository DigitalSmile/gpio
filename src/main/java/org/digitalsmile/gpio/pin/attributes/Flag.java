package org.digitalsmile.gpio.pin.attributes;

/**
 * GPIO Flags that can return from ioctl.
 */
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

    /**
     * Constructs Flag from given integer value.
     *
     * @param value - given integer value
     */
    Flag(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value from the Flag.
     *
     * @return integer value of the Flag
     */
    public int getValue() {
        return value;
    }
}
