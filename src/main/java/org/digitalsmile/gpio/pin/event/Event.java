package org.digitalsmile.gpio.pin.event;

import org.digitalsmile.gpio.pin.attributes.State;
import org.digitalsmile.gpio.pin.event.PinEventProcessing;

/**
 * Events, that you can subscribe and receive callback on the GPIO Pin state ({@link State}) change.
 *
 * @see PinEventProcessing
 */
public enum Event {
    /**
     * If the state was LOW and changed to HIGH.
     */
    RISING,
    /**
     * If the state was HIGH and changed to LOW.
     */
    FALLING,
    /**
     * if the state is changed.
     */
    BOTH;
}
