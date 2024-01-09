package org.digitalsmile.gpio.core.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;

public final class FileDescriptor {
    private static final Logger logger = LoggerFactory.getLogger(FileDescriptor.class);

    private static final SymbolLookup STD_LIB = Linker.nativeLinker().defaultLookup();
    private static final MethodHandle OPEN64 = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("open64").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
    private static final MethodHandle CLOSE = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("close").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
    private static final MethodHandle READ = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("read").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
    private static final MethodHandle WRITE = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("write").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    public static int open(String path, int openFlag) {
        logger.trace("Opening {}", path);
        var fd = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var str = offHeap.allocateUtf8String(path);
            fd = (int) OPEN64.invoke(str, openFlag);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if (fd == -1) {
            throw new RuntimeException("File " + path + " is not readable!");
        }
        logger.trace("Opened {} with file descriptor {}", path, fd);
        return fd;
    }

    public static int close(int fd) {
        logger.trace("Closing file descriptor {}", fd);
        try {
            var result = (int) CLOSE.invoke(fd);
            logger.trace("Closed file descriptor with result {}", result);
            return result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }



    public static int open(String path) {
        return open(path, Flags.O_RDWR);
    }

    public static byte[] read(int fd, int size) {
        logger.trace("Reading file descriptor {}", fd);
        var byteResult = new byte[]{};
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateArray(ValueLayout.JAVA_BYTE, new byte[size]);
            var read = (int) READ.invoke(fd, bufferMemorySegment, size);
            if (read != size) {
                throw new RuntimeException("Read " + read + " bytes, but size was " + size);
            }
            logger.trace("Read {} of {} bytes", read, size);
            byteResult = bufferMemorySegment.toArray(ValueLayout.JAVA_BYTE);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.trace("Read file descriptor {}", fd);
        return byteResult;
    }

    public static void write(int fd, byte[] data) {
        logger.trace("Writing to file descriptor {} with data {}", fd, Arrays.toString(data));
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateArray(ValueLayout.JAVA_BYTE, data);
            var wrote = (int) WRITE.invoke(fd, bufferMemorySegment, data.length);
            if (wrote != data.length) {
                throw new RuntimeException("Wrote " + wrote + " bytes, but size was " + data.length);
            }
            logger.trace("Wrote {} of {} bytes", wrote, data.length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.trace("Wrote to file descriptor {}", fd);
    }

}
