package org.digitalsmile.gpio.core;

public class IntegerToHex {
    private static final String digits = "0123456789abcdef";

    public static String convert(int input) {
        if (input <= 0) {
            return "0x0";
        }
        var hex = new StringBuilder();
        hex.append("0x");
        while (input > 0) {
            int digit = input % 16;
            hex.insert(0, digits.charAt(digit));
            input = input / 16;
        }
        return hex.toString();
    }
}
