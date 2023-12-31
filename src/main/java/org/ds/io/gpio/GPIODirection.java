package org.ds.io.gpio;

public enum GPIODirection {
    INPUT(1), OUTPUT(1 << 1);


    private final int mode;
    GPIODirection(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
