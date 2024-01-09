package org.digitalsmile.gpio.core;

/**
 * Helper class to convert from integer (byte) to hex string.
 */
public class IntegerToHex {
    private static final String digits = "0123456789abcdef";

    /**
     * Forbids creating an instance of this class.
     */
    private IntegerToHex() {
    }

    /**
     * Converts given integer (byte) input into hexadecimal string.
     *
     * @param input - integer (byte) input to convert
     * @return hexadecimal string representation of integer (byte)
     */
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
