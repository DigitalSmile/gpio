package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public record LineInfoStruct(int offset, int flags, byte[] name, byte[] consumer) implements NativeMemoryLayout {
    private final static MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("offset"),
            ValueLayout.JAVA_INT.withName("flags"),
            MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("name"),
            MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("consumer")
    );

    private static final VarHandle VH_OFFSET = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("offset"));
    private static final VarHandle VH_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("flags"));
    private static final MethodHandle MH_NAME = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("name"));
    private static final MethodHandle MH_CONSUMER = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("consumer"));

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineInfoStruct fromBytes(MemorySegment buffer) throws Throwable {
        return new LineInfoStruct(
                (int) VH_OFFSET.get(buffer),
                (int) VH_FLAGS.get(buffer),
                invokeExact(MH_NAME, buffer).toArray(ValueLayout.JAVA_BYTE),
                invokeExact(MH_CONSUMER, buffer).toArray(ValueLayout.JAVA_BYTE)
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_OFFSET.set(buffer, offset);
        VH_FLAGS.set(buffer, flags);
        var tmp = invokeExact(MH_NAME, buffer);
        for (int i = 0; i < name.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, name[i]);
        }
        tmp = invokeExact(MH_CONSUMER, buffer);
        for (int i = 0; i < consumer.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, consumer[i]);
        }
    }

    @Override
    public String toString() {
        return "GPIOLineInfo{" +
                "offset=" + offset +
                ", flags=" + flags +
                ", name=" + new String(name) +
                ", consumer=" + new String(consumer) +
                '}';
    }

    private static MemorySegment invokeExact(MethodHandle handle, MemorySegment buffer) throws Throwable {
        return ((MemorySegment) handle.invokeExact(buffer));
    }


}
