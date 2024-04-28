package org.digitalsmile.gpio.pin.event;

import org.digitalsmile.gpio.pin.attributes.PinEvent;

/**
 * Class-holder of detected pin events, which is filled upon successfully call of linux poll.
 *
 * @param timestamp      timestamp in nanoseconds (best effort estimate)
 * @param pinEvent       event type detected (rising of falling edge)
 * @param sequenceNumber number of event since detecting started on the pin
 */
public record DetectedEvent(long timestamp, PinEvent pinEvent, int sequenceNumber) {
}
