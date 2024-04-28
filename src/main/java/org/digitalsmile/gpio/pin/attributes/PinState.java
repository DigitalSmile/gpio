package org.digitalsmile.gpio.pin.attributes;

/**
 * GPIO Pin state, representing if it is HIGH (1) or LOW (0).
 */
public enum PinState {
    /**
     * Represents electrical 1 on the Pin
     */
    HIGH(1),
    /**
     * Represents electrical 0 of the Pin
     */
    LOW(0);

    private final int value;

    /**
     * Constructs the State from given integer value.
     *
     * @param state integer value of the State
     */
    PinState(int state) {
        this.value = state;
    }

    /**
     * Gets the byte value of State.
     * Please note, that byte and integer are identical types in java, so it is safe to cast from one to another.
     *
     * @return the byte value of state
     */
    public byte getValue() {
        return (byte) value;
    }
}
