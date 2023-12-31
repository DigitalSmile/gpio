package org.ds.io.spi;

public enum SPIMode {
    MODE_0(0),
    // CPHA
    MODE_1(0x01),
    // CPOL
    MODE_2(0x02),
    // CPHA | CPOL
    MODE_3(0x01 | 0x02);

    private final int value;
    SPIMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SPIMode getSPIMode(int value) {
        return switch (value) {
            case 0 -> MODE_0;
            case 0x01 -> MODE_1;
            case 0x02 -> MODE_2;
            case 0x01 | 0x02 -> MODE_3;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
