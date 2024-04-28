package org.digitalsmile.gpio.pin.attributes;

/**
 * Direction of a GPIO Pin - to read from (INPUT) or to write to (OUTPUT)
 */
public enum PinDirection {
    /**
     * INPUT direction aka read
     */
    INPUT(PinFlag.INPUT.getValue()),
    /**
     * OUTPUT direction aka write
     */
    OUTPUT(PinFlag.OUTPUT.getValue());


    private final int mode;

    /**
     * Constructs Direction from an integer mode.
     *
     * @param mode integer representation of direction
     */
    PinDirection(int mode) {
        this.mode = mode;
    }

    /**
     * Gets the integer mode of Direction.
     *
     * @return integer mode of Direction
     */
    public int getMode() {
        return mode;
    }
}
