package org.digitalsmile.gpio.i2c.attributes;

/**
 * Flags used for controlling I2CBus communications.
 */
public class Flag {

    /**
     * Forbids creating an instance of this class.
     */
    private Flag() {
    }

    /**
     * Maximum devices with 7 bit addressing map
     */
    public static final int MAX_7BIT_DEVICES = 127;
    /**
     * Device is busy
     */
    public static final int EBUSY = 16;

    /**
     * Maximum block size of message
     */
    public static final int  I2C_SMBUS_BLOCK_MAX	= 32;
    /**
     * Write byte of SMBus
     */
    public static final byte I2C_SMBUS_WRITE = 0;
    /**
     * Read byte of SMBus
     */
    public static final byte I2C_SMBUS_READ = 1;
    /**
     * Send WORD data
     */
    public static final byte I2C_SMBUS_WORD_DATA = 3;
    /**
     * Send BLOCK data
     */
    public static final byte I2C_SMBUS_I2C_BLOCK_DATA = 8;

}
