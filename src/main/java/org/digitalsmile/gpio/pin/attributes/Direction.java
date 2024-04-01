package org.digitalsmile.gpio.pin.attributes;

/**
 * Direction of a GPIO Pin - to read from (INPUT) or to write to (OUTPUT)
 */
public enum Direction {
    /**
     * INPUT direction aka read
     */
    INPUT(1),
    /**
     * OUTPUT direction aka write
     */
    OUTPUT(1 << 1);


    private final int mode;

    /**
     * Constructs Direction from an integer mode.
     *
     * @param mode integer representation of direction
     */
    Direction(int mode) {
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
