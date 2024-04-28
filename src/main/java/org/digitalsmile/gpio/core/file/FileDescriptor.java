package org.digitalsmile.gpio.core.file;

import org.digitalsmile.gpio.NativeMemoryException;
import org.digitalsmile.gpio.core.NativeMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;

/**
 * Class for calling simple operations (open, close, write, read) through native Java interface (FFM), introduced in recent versions of Java.
 * All methods are static and stateless. They are using standard kernel library (libc) calls to interact with native code.
 * Since this class is internal, the log level is set to trace.
 */
public final class FileDescriptor extends NativeMemory {
    private static final Logger logger = LoggerFactory.getLogger(FileDescriptor.class);

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
     * Opens file at path with selected flag ({@link FileFlag}).
     *
     * @param path     the file to open
     * @param openFlag flag to handle with file ({@link FileFlag})
     * @return file descriptor if file is successfully open
     * @throws NativeMemoryException when file path cannot be opened
     */
    public static int open(String path, int openFlag) throws NativeMemoryException {
        logger.trace("Opening {}", path);
        var fd = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var str = offHeap.allocateFrom(path);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            fd = (int) OPEN64.invoke(capturedState, str, openFlag);
            if (fd < 0) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Cannot open path '" + path + "': " + errnoStr.getString(0) + " (" + errno + ")", errno);
            }
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("Opened {} with file descriptor {}", path, fd);
        return fd;
    }

    /**
     * Opens file at path with flag {@link FileFlag}
     *
     * @param path the file to open
     * @return file descriptor if file is successfully open
     * @throws NativeMemoryException when file path cannot be opened
     */
    public static int open(String path) throws NativeMemoryException {
        return open(path, FileFlag.O_RDWR);
    }


    /**
     * Closes given file descriptor.
     *
     * @param fd file descriptor to close
     * @throws NativeMemoryException when file descriptor cannot be closed
     */
    public static void close(int fd) throws NativeMemoryException {
        logger.trace("Closing file descriptor {}", fd);
        try (Arena offHeap = Arena.ofConfined()) {
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var result = (int) CLOSE.invoke(capturedState, fd);
            if (result < 0) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Cannot close file descriptor '" + fd + "': " + errnoStr.getString(0) + " (" + errno + ")", errno);
            }
            logger.trace("Closed file descriptor with result {}", result);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
    }

    /**
     * Reads file descriptor with predefined size.
     *
     * @param fd   file descriptor to read
     * @param size size of the byte buffer to read into
     * @return byte array with contents of the read file descriptor
     * @throws NativeMemoryException when file descriptor cannot be read
     */
    public static byte[] read(int fd, int size) throws NativeMemoryException {
        logger.trace("Reading file descriptor {}", fd);
        var byteResult = new byte[size];
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateFrom(ValueLayout.JAVA_BYTE, byteResult);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            var read = (int) READ.invoke(capturedState, fd, bufferMemorySegment, size);
            if (read == -1) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Cannot read from file descriptor '" + fd + "': " + errnoStr.getString(0) + " (" + errno + ")", errno);
            }
            logger.trace("Read {} of {} bytes", read, size);
            byteResult = bufferMemorySegment.toArray(ValueLayout.JAVA_BYTE);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("Read file descriptor {}", fd);
        return byteResult;
    }

    /**
     * Writes byte array data to the provided file descriptor.
     *
     * @param fd   file descriptor to write
     * @param data byte array of data to write
     * @return wrote bytes
     * @throws NativeMemoryException when file descriptor cannot be written
     */
    public static int write(int fd, byte[] data) throws NativeMemoryException {
        logger.trace("Writing to file descriptor {} with data {}", fd, Arrays.toString(data));
        var wrote = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateFrom(ValueLayout.JAVA_BYTE, data);
            var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT);
            wrote = (int) WRITE.invoke(capturedState, fd, bufferMemorySegment, data.length);
            if (wrote == -1) {
                int errno = (int) ERRNO_HANDLE.get(capturedState);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Cannot write to file descriptor '" + fd + "': " + errnoStr.getString(0) + " (" + errno + ")", errno);
            }
            logger.trace("Wrote {} of {} bytes", wrote, data.length);
        } catch (NativeMemoryException e) {
            throw e;
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
        logger.trace("Wrote to file descriptor {}", fd);
        return wrote;
    }

    /**
     * Writes text data to the provided file descriptor.
     *
     * @param fd   file descriptor to write
     * @param data text data to write
     * @return wrote bytes
     * @throws NativeMemoryException when file descriptor cannot be written
     */
    public static int write(int fd, String data) throws NativeMemoryException {
        return write(fd, data.getBytes());
    }
}
