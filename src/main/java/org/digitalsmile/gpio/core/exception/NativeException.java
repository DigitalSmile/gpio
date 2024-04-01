package org.digitalsmile.gpio.core.exception;

/**
 * Exception class to handle IO errors of Native code.
 * This is just a wrapper for errno message which is broadcast from Throwable.
 */
public class NativeException extends Exception {

    /**
     * Error code for native exception
     */
    private int errorCode = -1;

    /**
     * Creates native exception with given root message and a cause.
     *
     * @param rootMessage root message of exception
     * @param cause       cause of exception
     */
    public NativeException(String rootMessage, Throwable cause) {
        super(rootMessage, cause);
    }

    /**
     * Creates native exception with given root message.
     *
     * @param rootMessage root message of exception
     */
    public NativeException(String rootMessage) {
        super(rootMessage);
    }

    /**
     * Creates native exception with given root message and a error code.
     *
     * @param rootMessage root message of exception
     * @param errorCode   error code
     */
    public NativeException(String rootMessage, int errorCode) {
        super(rootMessage);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code if any.
     *
     * @return error code
     */
    public int getErrorCode() {
        return errorCode;
    }
}
