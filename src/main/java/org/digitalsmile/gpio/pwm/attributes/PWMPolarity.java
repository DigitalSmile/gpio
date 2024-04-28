package org.digitalsmile.gpio.pwm.attributes;

import java.util.Arrays;

/**
 * PWM Polarity enum, that indicates whether impulses should be reversed.
 */
public enum PWMPolarity {
    /**
     * Normal polarity
     */
    NORMAL("normal"),
    /**
     * Inversed polarity
     */
    INVERSED("inversed");

    private final String polarity;

    /**
     * Creates PWM Polarity enum
     *
     * @param polarity string representation of polarity
     */
    PWMPolarity(String polarity) {
        this.polarity = polarity;
    }

    /**
     * Gets string representation of polarity.
     *
     * @return string representation of polarity
     */
    public String getPolarity() {
        return polarity;
    }

    /**
     * Gets PWM Polarity enum by string representation of polarity
     *
     * @param polarity string representation of polarity
     * @return PWM Polarity enum
     */
    public static PWMPolarity getPolarityByString(String polarity) {
        return Arrays.stream(values()).filter(p -> p.polarity.equals(polarity)).findFirst().orElseThrow();
    }
}
