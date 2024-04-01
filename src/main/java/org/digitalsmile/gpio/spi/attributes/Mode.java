package org.digitalsmile.gpio.spi.attributes;

/**
 * Class represents the modes of SPI bus.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Serial_Peripheral_Interface#Mode_numbers">SPI Modes on wikipedia</a>
 */
public enum Mode {
    /**
     * CPOL = 0
     * CPHA = 0
     */
    MODE_0(0),
    /**
     * CPOL = 0
     * CPHA = 1
     */
    MODE_1(0x01),
    /**
     * CPOL = 1
     * CPHA = 0
     */
    MODE_2(0x02),
    /**
     * CPOL = 1
     * CPHA = 1
     */
    MODE_3(0x01 | 0x02);

    private final int value;

    /**
     * Constructs SPI Mode from given integer value.
     *
     * @param value integer value of SPI Mode
     */
    Mode(int value) {
        this.value = value;
    }

    /**
     * Gets integer value of SPI Mode.
     *
     * @return integer value of SPI Mode
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets SPI mode by given integer value.
     *
     * @param value given integer value of spi mode
     * @return spi mode
     */
    public static Mode getSPIMode(int value) {
        return switch (value) {
            case 0 -> MODE_0;
            case 0x01 -> MODE_1;
            case 0x02 -> MODE_2;
            case 0x01 | 0x02 -> MODE_3;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
