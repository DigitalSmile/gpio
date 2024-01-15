package org.digitalsmile.gpio.core.exception;

/**
 * Exception class to handle IO errors of Native code.
 * This is just a wrapper for errno message which is broadcasted from Throwable.
 */
public class NativeException extends Exception {

    /**
     * Creates native exception with given root message and a cause.
     *
     * @param rootMessage root message of exception
     * @param cause       cause of exception
     */
    public NativeException(String rootMessage, Throwable cause) {
        super(rootMessage, cause);
    }
}
