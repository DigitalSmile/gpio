package org.digitalsmile.gpio.core.ioctl;

import org.digitalsmile.gpio.core.IntegerToHex;
import org.digitalsmile.gpio.core.NativeMemory;
import org.digitalsmile.gpio.NativeMemoryException;
import org.digitalsmile.gpio.core.NativeMemoryLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;


/**
 * Class for calling ioctl through native Java interface (FFM), introduced in recent versions of Java.
 * All methods are static and stateless. They are using standard kernel library (libc) calls to interact with native code.
 * Since this class is internal, the log level is set to trace.
 */
public final class IOCtl extends NativeMemory {
    private static final Logger logger = LoggerFactory.getLogger(IOCtl.class);

    // usage by pointer to structure
    private static final MethodHandle IOCTL = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("ioctl").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            Linker.Option.captureCallState("errno"));

    // usage by value
    private static final MethodHandle IOCTL_VALUE = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("ioctl").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
            Linker.Option.captureCallState("errno"));

    /**
     * Forbids creating an instance of this class.
     */
    private IOCtl() {
    }

    /**
     * Calls ioctl with given file descriptor, command ({@link Command}) and integer data by value.
     * Please note, that byte and integer are identical types in java, so it is safe to cast from one to another.
     *
     * @param fd      file descriptor to call
     * @param command command to operate ({@link Command})
     * @param data    integer (byte) data to send
     * @return the result of calling ioctl operation, if any or zero if there is no data filled in
     * @throws NativeMemoryException when call to ioctl returns error
     */
    public static long callByValue(int fd, long command, long data) throws NativeMemoryException {
        logger.trace("ioctl writing to file descriptor {}, command {} and data {}", fd, IntegerToHex.convert(command), IntegerToHex.convert(data));
        var result = 0L;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(ValueLayout.JAVA_LONG);
            bufferMemorySegment.set(ValueLayout.JAVA_LONG, 0, data);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var callResult = (int) IOCTL_VALUE.invoke(capturedState, fd, command, data);
            processError(callResult, fd, command, data, capturedState);
            logger.trace("ioctl call return {}", callResult);
            result = bufferMemorySegment.get(ValueLayout.JAVA_LONG, 0);
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("ioctl call result {}", result);
        return result;
    }

    /**
     * Calls ioctl with given file descriptor, command ({@link Command}) and integer data as pointer.
     * Please note, that byte and integer are identical types in java, so it is safe to cast from one to another.
     *
     * @param fd      file descriptor to call
     * @param command command to operate ({@link Command})
     * @param data    integer (byte) data to send
     * @return the result of calling ioctl operation, if any or zero if there is no data filled in
     * @throws NativeMemoryException when call to ioctl returns error
     */
    public static long call(int fd, long command, long data) throws NativeMemoryException {
        logger.trace("ioctl writing to file descriptor {}, command {} and data {}", fd, IntegerToHex.convert(command), IntegerToHex.convert(data));
        var result = 0L;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(ValueLayout.JAVA_LONG);
            bufferMemorySegment.set(ValueLayout.JAVA_LONG, 0, data);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var callResult = (int) IOCTL.invoke(capturedState, fd, command, bufferMemorySegment);
            processError(callResult, fd, command, data, capturedState);
            logger.trace("ioctl call return {}", callResult);
            result = bufferMemorySegment.get(ValueLayout.JAVA_LONG, 0);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("ioctl call result {}", result);
        return result;
    }

    /**
     * Calls ioctl with given file descriptor, command ({@link Command}) and integer data.
     * Please note, that byte and integer are identical types in java, so it is safe to cast from one to another.
     *
     * @param fd      file descriptor to call
     * @param command command to operate ({@link Command})
     * @param data    integer (byte) data to send
     * @return the result of calling ioctl operation, if any or zero if there is no data filled in
     * @throws NativeMemoryException when call to ioctl returns error
     */
    public static int call(int fd, long command, int data) throws NativeMemoryException {
        logger.trace("ioctl writing to file descriptor {}, command {} and data {}", fd, IntegerToHex.convert(command), IntegerToHex.convert(data));
        var result = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(ValueLayout.JAVA_INT);
            bufferMemorySegment.set(ValueLayout.JAVA_INT, 0, data);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var callResult = (int) IOCTL.invoke(capturedState, fd, command, bufferMemorySegment);
            processError(callResult, fd, command, data, capturedState);
            logger.trace("ioctl call return {}", callResult);
            result = bufferMemorySegment.get(ValueLayout.JAVA_INT, 0);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("ioctl call result {}", result);
        return result;
    }

    /**
     * Process the error and raise exception method.
     *
     * @param callResult    result of the call
     * @param fd            file descriptor
     * @param command       command
     * @param data          data
     * @param capturedState state of errno
     * @throws NativeMemoryException if call result is -1
     */
    private static void processError(long callResult, int fd, long command, long data, MemorySegment capturedState) throws NativeMemoryException {
        if (callResult == -1) {
            try {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Error during ioctl call with file descriptor '" + fd + "', command '" +
                        IntegerToHex.convert(command) + "' and data '" + IntegerToHex.convert(data) + "': " +
                        errnoStr.getUtf8String(0) + " (" + errno + ")", errno);
            } catch (Throwable e) {
                throw new NativeMemoryException(e.getMessage(), e);
            }
        }
    }

    /**
     * Calls ioctl with given file descriptor, command ({@link Command}) and specific data class.
     * The data class must extend {@link NativeMemoryLayout} interface and provide layout information, sa well as methods to convert to bytes and vice versa.
     *
     * @param fd      file descriptor to call
     * @param command command to operate ({@link Command})
     * @param data    data structure instance
     * @param <T>     data structure class, that provides layout information and conversion methods, must implements {@link NativeMemoryLayout}
     * @return filled structure instance of type T
     * @throws NativeMemoryException when call to ioctl returns error
     */
    public static <T extends NativeMemoryLayout> T call(int fd, long command, T data) throws NativeMemoryException {
        logger.trace("ioctl access with file descriptor {}, command {} and data {}", fd, Long.toHexString(command), data);
        T result;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(data.getMemoryLayout());
            data.toBytes(bufferMemorySegment);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var callResult = (int) IOCTL.invoke(capturedState, fd, command, bufferMemorySegment);
            if (callResult == -1) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Error during ioctl call with file descriptor '" + fd + "', command '" +
                        IntegerToHex.convert(command) + "' and data '" + data + "': " +
                        errnoStr.getUtf8String(0) + " (" + errno + ")", errno);
            }
            logger.trace("ioctl call return {}", callResult);
            result = data.fromBytes(bufferMemorySegment);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("ioctl call result {}", result);
        return result;
    }
}
