package org.digitalsmile.gpio.pin.attributes;

public enum Direction {
    INPUT(1), OUTPUT(1 << 1);


    private final int mode;
    Direction(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
