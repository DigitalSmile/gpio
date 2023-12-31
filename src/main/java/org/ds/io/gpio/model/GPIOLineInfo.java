package org.ds.io.gpio.model;

import org.ds.io.core.NativeMemoryAccess;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public record GPIOLineInfo(int offset, int flags, byte[] name, byte[] consumer) implements NativeMemoryAccess {
    private static MemoryLayout LAYOUT = MemoryLayout.structLayout(
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

    @Override
    public GPIOLineInfo fromBytes(MemorySegment buffer) throws Throwable {
        return new GPIOLineInfo(
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

    //    public static GPIOLineInfo fromBytes(byte[] buffer) {
//        var d = new DataInputStream(new ByteArrayInputStream(buffer));
//        try {
//            return new GPIOLineInfo(
//                    d.readUnsignedByte(),
//                    d.readUnsignedByte(),
//                    new String(d.readNBytes(32)),
//                    new String(d.readNBytes(32))
//            );
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static ByteBuffer toBytes(int offset, int flags, String name, String consumer) {
//        var byteBuffer = ByteBuffer.allocate((int) allocationSize());
//        byteBuffer.put((byte) offset);
//        byteBuffer.putInt(flags);
//        byteBuffer.put(name.getBytes());
//        byteBuffer.put(consumer.getBytes());
//        return byteBuffer;
//    }


}
