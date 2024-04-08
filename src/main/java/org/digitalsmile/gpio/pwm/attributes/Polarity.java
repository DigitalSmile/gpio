package org.digitalsmile.gpio.pwm.attributes;

import java.util.Arrays;

public enum Polarity {
    NORMAL("normal"), INVERSED("inversed");

    private final String polarity;
    Polarity(String polarity) {
        this.polarity = polarity;
    }

    public String getPolarity() {
        return polarity;
    }

    public static Polarity getPolarityByString(String polarity) {
        return Arrays.stream(values()).filter(p -> p.polarity.equals(polarity)).findFirst().orElseThrow();
    }
}
