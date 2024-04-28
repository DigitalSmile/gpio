package org.digitalsmile.gpio.core.poll;

import org.digitalsmile.gpio.core.NativeMemory;
import org.digitalsmile.gpio.NativeMemoryException;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Class for calling poll through native Java interface (FFM), introduced in recent versions of Java.
 * All methods are static and stateless. They are using standard kernel library (libc) calls to interact with native code.
 * Since this class is internal, the log level is set to trace.
 */
public final class Poll extends NativeMemory {
    private static final Logger logger = LoggerFactory.getLogger(FileDescriptor.class);

    private static final MethodHandle POLL = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("poll").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
            Linker.Option.captureCallState("errno"));

    /**
     * Forbids creating an instance of this class.
     */
    private Poll() {
    }

    /**
     * Polls the given file descriptor data, number of file descriptors and timeout. If timeout occurred, the method will return null.
     *
     * @param pollingData data for storing file descriptors and interested events
     * @param size        number of file descriptors in data
     * @param timeout     timeout in calling poll in milliseconds
     * @return filled data with events to be processed or null if timeout occurs
     * @throws NativeMemoryException when call to poll returns error
     */
    public static PollingData poll(PollingData pollingData, int size, int timeout) throws NativeMemoryException {
        logger.trace("Polling file descriptor '{}' with timeout {}ms", pollingData.fd(), timeout);
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(pollingData.getMemoryLayout());
            pollingData.toBytes(bufferMemorySegment);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var callResult = (int) POLL.invoke(capturedState, bufferMemorySegment, size, timeout);
            if (callResult < 0) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Polling of '" + pollingData.fd() + "' error: " + errnoStr.getUtf8String(0) + " (" + errno + ")", errno);
            }
            if (callResult == 0) {
                logger.trace("Polling file descriptor '{}' timed out.", pollingData.fd());
                return null;
            }
            logger.trace("Events detected on file descriptor '{}'.", pollingData.fd());
            return pollingData.fromBytes(bufferMemorySegment);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
    }
}
