package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Structure the represents GPIO data to send with ioctl.
 *
 * @param lineOffsets   line offset
 * @param flags         flags
 * @param defaultValues default values
 * @param consumerLabel consumer labels
 * @param lines         lines
 * @param fd            file descriptor
 */
public record HandleRequestStruct(int[] lineOffsets, int flags, byte[] defaultValues, byte[] consumerLabel, int lines,
                                  int fd) implements NativeMemoryLayout {

    // see https://elixir.bootlin.com/linux/v6.7/source/include/uapi/linux/gpio.h#L412
    private static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(64, ValueLayout.JAVA_INT).withName("lineOffsets"),
            ValueLayout.JAVA_INT.withName("flags"),
            MemoryLayout.sequenceLayout(64, ValueLayout.JAVA_BYTE).withName("defaultValues"),
            MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("consumerLabel"),
            ValueLayout.JAVA_INT.withName("lines"),
            ValueLayout.JAVA_INT.withName("fd")
    );

    private static final MethodHandle MH_LINE_OFFSETS = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("lineOffsets"));
    private static final VarHandle VH_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("flags"));
    private static final MethodHandle MH_DEFAULT_VALUES = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("defaultValues"));
    private static final MethodHandle MH_CONSUMER_LABEL = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("consumerLabel"));
    private static final VarHandle VH_LINES = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("lines"));
    private static final VarHandle VH_FD = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fd"));


    /**
     * Helper method to create structure with pin, mode and consumer label.
     *
     * @param pin   the pin
     * @param mode  the mode
     * @param label the label
     * @return HandleRequestStruct structure
     */
    public static HandleRequestStruct createEmpty(int pin, int mode, String label) {
        return new HandleRequestStruct(new int[]{pin}, mode, new byte[]{}, label.getBytes(), 1, 0);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    private static MemorySegment invokeExact(MethodHandle handle, MemorySegment buffer) throws Throwable {
        return (MemorySegment) handle.invokeExact(buffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public HandleRequestStruct fromBytes(MemorySegment buffer) throws Throwable {
        return new HandleRequestStruct(
                invokeExact(MH_LINE_OFFSETS, buffer).toArray(ValueLayout.JAVA_INT),
                (int) VH_FLAGS.get(buffer),
                invokeExact(MH_DEFAULT_VALUES, buffer).toArray(ValueLayout.JAVA_BYTE),
                invokeExact(MH_CONSUMER_LABEL, buffer).toArray(ValueLayout.JAVA_BYTE),
                (int) VH_LINES.get(buffer),
                (int) VH_FD.get(buffer)
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        var tmp = invokeExact(MH_LINE_OFFSETS, buffer);
        for (int i = 0; i < lineOffsets.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, lineOffsets[i]);
        }
        VH_FLAGS.set(buffer, flags);
        tmp = invokeExact(MH_DEFAULT_VALUES, buffer);
        for (int i = 0; i < defaultValues.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_BYTE, i, defaultValues[i]);
        }
        tmp = invokeExact(MH_CONSUMER_LABEL, buffer);
        for (int i = 0; i < consumerLabel.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_BYTE, i, consumerLabel[i]);
        }
        VH_LINES.set(buffer, lines);
        VH_FD.set(buffer, fd);
    }

    @Override
    public String toString() {
        return "GPIOHandleRequest{" +
                "lineOffsets=" + Arrays.toString(lineOffsets) +
                ", flags=" + flags +
                ", defaultValues=" + new String(defaultValues) +
                ", consumerLabel=" + new String(consumerLabel) +
                ", lines=" + lines +
                ", fd=" + fd +
                '}';
    }
}
