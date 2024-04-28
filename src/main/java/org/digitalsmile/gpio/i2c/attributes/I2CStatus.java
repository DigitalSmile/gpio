package org.digitalsmile.gpio.i2c.attributes;

/**
 * Status of devices on the bus.
 */
public enum I2CStatus {
    /**
     * Device is available and ready to communicate.
     */
    AVAILABLE,
    /**
     * Device is busy by some other activities.
     */
    BUSY,
    /**
     * Device status is unknown, most probably the error occurred while communicating the device.
     */
    UNKNOWN,
    /**
     * Device is unavailable.
     */
    NOT_AVAILABLE;
}
