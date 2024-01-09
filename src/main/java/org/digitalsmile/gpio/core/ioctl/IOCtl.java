package org.digitalsmile.gpio.core.ioctl;

import org.digitalsmile.gpio.core.IntegerToHex;
import org.digitalsmile.gpio.core.NativeMemoryLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class IOCtl {
    private static final Logger logger = LoggerFactory.getLogger(IOCtl.class);

    private static final SymbolLookup STD_LIB = Linker.nativeLinker().defaultLookup();
    private static final MethodHandle IOCTL = Linker.nativeLinker().downcallHandle(
            STD_LIB.find("ioctl").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    public static int call(int fd, long command, int data) {
        logger.trace("ioctl writing to file descriptor {}, command {} and data {}", fd, Long.toHexString(command), IntegerToHex.convert(data));
        var result = 0;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(ValueLayout.JAVA_INT);
            bufferMemorySegment.set(ValueLayout.JAVA_INT, 0, data);
            var callResult = (int) IOCTL.invoke(fd, command, bufferMemorySegment);
            if (callResult == -1) {
                throw new RuntimeException("Result calling of ioctl is -1");
            }
            logger.trace("ioctl call return {}", callResult);
            result = bufferMemorySegment.get(ValueLayout.JAVA_INT, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.trace("ioctl call result {}", result);
        return result;
    }

    public static <T extends NativeMemoryLayout> T call(int fd, long command, T data) {
        logger.trace("ioctl access with file descriptor {}, command {} and data {}", fd, Long.toHexString(command), data);
        T result;
        try (Arena offHeap = Arena.ofConfined()) {
            var bufferMemorySegment = offHeap.allocate(data.getMemoryLayout());
            data.toBytes(bufferMemorySegment);
            var callResult = (int) IOCTL.invoke(fd, command, bufferMemorySegment);
            if (callResult == -1) {
                throw new RuntimeException("Result calling of ioctl is -1");
            }
            logger.trace("ioctl call return {}", callResult);
            result = data.fromBytes(bufferMemorySegment);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.trace("ioctl call result {}", result);
        return result;
    }
}
