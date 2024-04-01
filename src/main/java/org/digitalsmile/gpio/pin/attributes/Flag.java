package org.digitalsmile.gpio.pin.attributes;

/**
 * GPIO Flags that can return from ioctl.
 */
public enum Flag {
    /**
     * Check if pin is used by kernel
     */
    KERNEL(1),  // 1
    /**
     * Check if pin is out
     */
    IS_OUT(1 << 1), // 2
    /**
     * Check if pin is active low
     */
    ACTIVE_LOW(1 << 2), // 4
    /**
     * Check if pin is open drain
     */
    OPEN_DRAIN(1 << 3), // 8
    /**
     * Check if pin is open source
     */
    OPEN_SOURCE(1 << 4), // 16
    /**
     * Check if pin is biased pulled up
     */
    BIAS_PULL_UP(1 << 5), // 32
    /**
     * Check if pin is biased pulled down
     */
    BIAS_PULL_DOWN(1 << 6), // 64
    /**
     * Check if pin is biased disabled
     */
    BIAS_DISABLE(1 << 7); // 128;


    private final int value;

    /**
     * Constructs Flag from given integer value.
     *
     * @param value given integer value
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
