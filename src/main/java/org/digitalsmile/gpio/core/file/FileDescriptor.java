package org.digitalsmile.gpio.core.file;

import org.digitalsmile.gpio.core.exception.NativeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Class for calling simple operations (open, close, write, read) through native Java interface (FFM), introduced in recent versions of Java.
 * All methods are static and stateless. They are using standard kernel library (libc) calls to interact with native code.
 * Since this class is internal, the log level is set to trace.
 */
public final class FileDescriptor {
    private static final Logger logger = LoggerFactory.getLogger(FileDescriptor.class);

    private static final SymbolLookup STD_LIB = Linker.nativeLinker().defaultLookup();
    private static final StructLayout CAPTURED_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle ERRNO_HANDLE = CAPTURED_STATE_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("errno"));

    private static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(
            MemoryLayout.sequenceLayout(ValueLayout.JAVA_BYTE));
    private static final MethodHandle STR_ERROR = Linker.nativeLinker().downcallHandle(
            Linker.nativeLinker().defaultLookup().find("strerror").orElseThrow(),
            FunctionDescriptor.of(POINTER, ValueLayout.JAVA_INT));

    private static final MethodHandle OPEN64 = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("open64").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
            Linker.Option.captureCallState("errno"));
    private static final MethodHandle CLOSE = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("close").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
            Linker.Option.captureCallState("errno"));
    private static final MethodHandle READ = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("read").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
            Linker.Option.captureCallState("errno"));
    private static final MethodHandle WRITE = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("write").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
            Linker.Option.captureCallState("errno"));

    /**
     * Forbids creating an instance of this class.
     */
    private FileDescriptor() {
    }

    /**
     * Opens file at path with selected flag ({@link Flag}).
     *
     * @param path     - the file to open
     * @param openFlag - flag to handle with file ({@link Flag})
     * @return file descriptor if file is successfully open
     * @throws NativeException when file path cannot be opened
     */
    public static int open(String path, int openFlag) throws NativeException {
        logger.trace("Opening {}", path);
        var fd = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var str = offHeap.allocateUtf8String(path);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            fd = (int) OPEN64.invoke(capturedState, str, openFlag);
            if (fd < 0) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new RuntimeException("Cannot open path '" + path + "': " + errnoStr.getUtf8String(0) + " (" + errno + ")");
            }
        } catch (Throwable e) {
            throw new NativeException(e.getMessage(), e);
        }
        logger.trace("Opened {} with file descriptor {}", path, fd);
        return fd;
    }

    /**
     * Opens file at path with flag {@link Flag}
     *
     * @param path - the file to open
     * @return file descriptor if file is successfully open
     * @throws NativeException when file path cannot be opened
     */
    public static int open(String path) throws NativeException {
        return open(path, Flag.O_RDWR);
    }


    /**
     * Closes given file descriptor.
     *
     * @param fd - file descriptor to close
     * @throws NativeException when file descriptor cannot be closed
     */
    public static void close(int fd) throws NativeException {
        logger.trace("Closing file descriptor {}", fd);
        try (Arena offHeap = Arena.ofConfined()) {
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var result = (int) CLOSE.invoke(capturedState, fd);
            if (result < 0) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new RuntimeException("Cannot close file descriptor '" + fd + "': " + errnoStr.getUtf8String(0) + " (" + errno + ")");
            }
            logger.trace("Closed file descriptor with result {}", result);
        } catch (Throwable e) {
            throw new NativeException(e.getMessage(), e);
        }
    }

    /**
     * Reads file descriptor with predefined size.
     *
     * @param fd   - file descriptor to read
     * @param size - size of the byte buffer to read into
     * @return byte array with contents of the read file descriptor
     * @throws NativeException when file descriptor cannot be read
     */
    public static byte[] read(int fd, int size) throws NativeException {
        logger.trace("Reading file descriptor {}", fd);
        var byteResult = new byte[size];
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateArray(ValueLayout.JAVA_BYTE, byteResult);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var read = (int) READ.invoke(capturedState, fd, bufferMemorySegment, size);
            if (read == -1) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new RuntimeException("Cannot read from file descriptor '" + fd + "': " + errnoStr.getUtf8String(0) + " (" + errno + ")");
            }
            logger.trace("Read {} of {} bytes", read, size);
            byteResult = bufferMemorySegment.toArray(ValueLayout.JAVA_BYTE);
        } catch (Throwable e) {
            throw new NativeException(e.getMessage(), e);
        }
        logger.trace("Read file descriptor {}", fd);
        return byteResult;
    }

    /**
     * Writes byte array data to the provided file descriptor.
     *
     * @param fd   - file descriptor to write
     * @param data - byte array of data to write
     * @throws NativeException when file descriptor cannot be written
     */
    public static void write(int fd, byte[] data) throws NativeException {
        logger.trace("Writing to file descriptor {} with data {}", fd, Arrays.toString(data));
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateArray(ValueLayout.JAVA_BYTE, data);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var wrote = (int) WRITE.invoke(capturedState, fd, bufferMemorySegment, data.length);
            if (wrote == -1) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new RuntimeException("Cannot write to file descriptor '" + fd + "': " + errnoStr.getUtf8String(0) + " (" + errno + ")");
            }
            logger.trace("Wrote {} of {} bytes", wrote, data.length);
        } catch (Throwable e) {
            throw new NativeException(e.getMessage(), e);
        }
        logger.trace("Wrote to file descriptor {}", fd);
    }
}
