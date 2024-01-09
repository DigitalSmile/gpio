package org.digitalsmile.gpio.pin;

import org.digitalsmile.gpio.pin.attributes.State;

@FunctionalInterface
public interface PinEventProcessing {
    void process(State state);
}
