package org.ds.io.gpio;

public enum GPIOState {
    HIGH((byte) 1), LOW((byte) 0);

    private final byte state;
    GPIOState(byte state) {
        this.state = state;
    }

    public byte getState() {
        return state;
    }
}
