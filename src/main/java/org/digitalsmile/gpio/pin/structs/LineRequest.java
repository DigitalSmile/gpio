package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Information about a request for GPIO lines.
 *
 * @param offsets         an array of desired lines, specified by offset index for the associated GPIO chip
 * @param consumer        a desired consumer label for the selected GPIO lines such as "my-bitbanged-relay"
 * @param config          requested configuration for the lines
 * @param numLines        number of lines requested in this request
 * @param eventBufferSize a suggested minimum number of line events that the kernel should buffer. This is only relevant if edge detection is enabled in the configuration. Note that this is only a suggested value and the kernel may allocate a larger buffer or cap the size of the buffer. If this field is zero then the buffer size defaults to a minimum of numLines * 16.
 * @param fd              after a successful IOCTL operation, contains a valid anonymous file descriptor representing the request
 */
public record LineRequest(int[] offsets, byte[] consumer, LineConfig config, int numLines, int eventBufferSize,
                          int fd) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(64, ValueLayout.JAVA_INT).withName("offsets"),
            MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("consumer"),
            LineConfig.LAYOUT.withName("config"),
            ValueLayout.JAVA_INT.withName("num_lines"),
            ValueLayout.JAVA_INT.withName("event_buffer_size"),
            MemoryLayout.sequenceLayout(5, ValueLayout.JAVA_INT).withName("padding"),
            ValueLayout.JAVA_INT.withName("fd")
    );
    private static final MethodHandle MH_OFFSETS = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("offsets"));
    private static final MethodHandle MH_CONSUMER = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("consumer"));
    private static final MethodHandle MH_CONFIG = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("config"));
    private static final VarHandle VH_NUM_LINES = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("num_lines"));
    private static final VarHandle VH_EVENT_BUFFER_SIZE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("event_buffer_size"));
    private static final MethodHandle MH_PADDING = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("padding"));
    private static final VarHandle VH_FD = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fd"));


    /**
     * Creates Line Request by given offsets, consumer and Line config
     *
     * @param offsets  offsets array
     * @param consumer consumer string
     * @param config   line config
     * @return Line Request instance
     */
    public static LineRequest create(int[] offsets, String consumer, LineConfig config) {
        return new LineRequest(offsets, consumer.getBytes(), config, offsets.length, 0, 0);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineRequest fromBytes(MemorySegment buffer) throws Throwable {
        var lineConfigMemoryBuffer = (MemorySegment) MH_CONFIG.invokeExact(buffer);
        var lineConfig = LineConfig.createEmpty().fromBytes(lineConfigMemoryBuffer);
        return new LineRequest(
                ((MemorySegment) MH_OFFSETS.invokeExact(buffer)).toArray(ValueLayout.JAVA_INT),
                ((MemorySegment) MH_CONSUMER.invokeExact(buffer)).toArray(ValueLayout.JAVA_BYTE),
                lineConfig,
                (int) VH_NUM_LINES.get(buffer),
                (int) VH_EVENT_BUFFER_SIZE.get(buffer),
                (int) VH_FD.get(buffer)
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        var tmp = (MemorySegment) MH_OFFSETS.invokeExact(buffer);
        for (int i = 0; i < offsets.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, offsets[i]);
        }
        tmp = (MemorySegment) MH_CONSUMER.invokeExact(buffer);
        for (int i = 0; i < consumer.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_BYTE, i, consumer[i]);
        }
        tmp = (MemorySegment) MH_CONFIG.invokeExact(buffer);
        config.toBytes(tmp);
        VH_NUM_LINES.set(buffer, numLines);
        VH_EVENT_BUFFER_SIZE.set(buffer, eventBufferSize);
        tmp = (MemorySegment) MH_PADDING.invokeExact(buffer);
        for (int i = 0; i < 5; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, 0);
        }
        VH_FD.set(buffer, fd);
    }

    @Override
    public String toString() {
        return "LineRequest{" +
                "offsets=" + Arrays.toString(offsets) +
                ", consumer=(" + new String(consumer).trim() + ")" + Arrays.toString(consumer) +
                ", config=" + config +
                ", numLines=" + numLines +
                ", eventBufferSize=" + eventBufferSize +
                ", fd=" + fd +
                '}';
    }
}
