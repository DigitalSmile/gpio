package org.digitalsmile.gpio.pin.event;

import org.digitalsmile.gpio.pin.attributes.State;

/**
 * Event processing callback. Can be used as functional interface in streams.
 */
@FunctionalInterface
public interface PinEventProcessing {
    /**
     * Process the changed on the GPIO Pin.
     *
     * @param state new state of GPIO Pin
     */
    void process(State state);
}
