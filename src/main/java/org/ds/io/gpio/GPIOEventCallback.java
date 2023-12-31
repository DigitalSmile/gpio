package org.ds.io.gpio;

@FunctionalInterface
public interface GPIOEventCallback {
    void eventDetected(GPIOState state);
}
