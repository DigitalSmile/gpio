package org.digitalsmile.gpio.pin.attributes;

public enum State {
    HIGH((byte) 1), LOW((byte) 0);

    private final byte state;
    State(byte state) {
        this.state = state;
    }

    public byte getState() {
        return state;
    }
}
