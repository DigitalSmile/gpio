package org.ds.io.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;

public class NativeCaller {
    private static final Logger logger = LoggerFactory.getLogger(NativeCaller.class);

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
    private static final MethodHandle IOCTL = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("ioctl").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    public static int close(int fd) {
        logger.debug("Closing file descriptor {}", fd);
        try {
            var result = (int) CLOSE.invoke(fd);
            logger.debug("Closed file descriptor with result {}", result);
            return result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int open(String path, int openFlag) {
        logger.debug("Opening {}", path);
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
        logger.debug("Opened {} with file descriptor {}", path, fd);
        return fd;
    }

    public static int open(String path) {
        return open(path, 2);
    }

    public static byte[] read(int fd, int size) {
        logger.debug("Reading file descriptor {}", fd);
        var byteResult = new byte[]{};
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateArray(ValueLayout.JAVA_BYTE, new byte[size]);
            var read = (int) READ.invoke(fd, bufferMemorySegment, size);
            if (read != size) {
                throw new RuntimeException("Read " + read + " bytes, but size was " + size);
            }
            logger.debug("Read {} of {} bytes", read, size);
            byteResult = bufferMemorySegment.toArray(ValueLayout.JAVA_BYTE);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.debug("Read file descriptor {}", fd);
        return byteResult;
    }

    public static void write(int fd, byte[] data) {
        logger.debug("Writing to file descriptor {} with data {}", fd, Arrays.toString(data));
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocateArray(ValueLayout.JAVA_BYTE, data);
            var wrote = (int) WRITE.invoke(fd, bufferMemorySegment, data.length);
            if (wrote != data.length) {
                throw new RuntimeException("Wrote " + wrote + " bytes, but size was " + data.length);
            }
            logger.debug("Wrote {} of {} bytes", wrote, data.length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.debug("Wrote to file descriptor {}", fd);
    }

    public static int ioctl(int fd, long command, int data) {
        logger.debug("ioctl writing to file descriptor {}, command {} and data {}", fd, Long.toHexString(command), integerToHex(data));
        var result = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(ValueLayout.JAVA_INT);
            bufferMemorySegment.set(ValueLayout.JAVA_INT, 0, data);
            var callResult = (int) IOCTL.invoke(fd, command, bufferMemorySegment);
            if (callResult == -1) {
                throw new RuntimeException("Result calling of ioctl is -1");
            }
            logger.debug("ioctl call return {}", callResult);
            result = bufferMemorySegment.get(ValueLayout.JAVA_INT, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.debug("ioctl call result {}", result);
        return result;
    }

    public static <T extends NativeMemoryAccess> T ioctl(int fd, long command, T data) {
        //logger.debug("ioctl access with file descriptor {}, command {} and data {}", fd, Long.toHexString(command), data);
        T result;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(data.getMemoryLayout());
            data.toBytes(bufferMemorySegment);
            var callResult = (int) IOCTL.invoke(fd, command, bufferMemorySegment);
            if (callResult == -1) {
                throw new RuntimeException("Result calling of ioctl is -1");
            }
            //logger.debug("ioctl call return {}", callResult);
            result = data.fromBytes(bufferMemorySegment);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        //logger.debug("ioctl call result {}", result);
        return result;
    }

    private static final String digits = "0123456789ABCDEF";
    private static String integerToHex(int input) {
        if (input <= 0)
            return "0";
        StringBuilder hex = new StringBuilder();
        while (input > 0) {
            int digit = input % 16;
            hex.insert(0, digits.charAt(digit));
            input = input / 16;
        }
        return hex.toString();
    }
}
