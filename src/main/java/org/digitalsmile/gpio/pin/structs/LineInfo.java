package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Information about a certain GPIO line.
 *
 * @param name     the name of this GPIO line, such as the output pin of the line on the chip, a rail or a pin header name on a board, as specified by the GPIO chip, may be empty (i.e. name[0] == '\0')
 * @param consumer a functional name for the consumer of this GPIO line as set by whatever is using it, will be empty if there is no current user but may also be empty if the consumer doesn't set this up
 * @param offset   the local offset on this GPIO chip, fill this in when requesting the line information from the kernel
 * @param numAttrs the number of attributes
 * @param flags    flags for this GPIO line
 * @param attrs    the configuration attributes associated with the line
 */
public record LineInfo(byte[] name, byte[] consumer, int offset, int numAttrs, long flags,
                       LineAttribute... attrs) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("name"),
            MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("consumer"),
            ValueLayout.JAVA_INT.withName("offset"),
            ValueLayout.JAVA_INT.withName("numAttrs"),
            ValueLayout.JAVA_LONG.withName("flags"),
            MemoryLayout.sequenceLayout(10, LineAttribute.LAYOUT).withName("attrs"),
            MemoryLayout.sequenceLayout(4, ValueLayout.JAVA_INT).withName("padding")
    );
    private static final MethodHandle MH_NAME = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("name"));
    private static final MethodHandle MH_CONSUMER = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("consumer"));
    private static final VarHandle VH_OFFSET = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("offset"));
    private static final VarHandle VH_NUM_ATTRS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("numAttrs"));
    private static final VarHandle VH_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("flags"));
    private static final MethodHandle MH_ATTRS = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("attrs"));
    private static final MethodHandle MH_PADDING = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("padding"));

    /**
     * Creates Line Info instance by given pin.
     *
     * @param pin pin for creating instance
     * @return Line Info instance
     */
    public static LineInfo create(int pin) {
        return new LineInfo(new byte[]{}, new byte[]{}, pin, 0, 0);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineInfo fromBytes(MemorySegment buffer) throws Throwable {
        var numAttrs = (int) VH_NUM_ATTRS.get(buffer, 0L);
        var attrsMemorySegment = (MemorySegment) MH_ATTRS.invokeExact(buffer, 0L);
        var attrs = new LineAttribute[numAttrs];
        for (int i = 0; i < numAttrs; i++) {
            var attr = LineAttribute.createEmpty();
            attrs[i] = attr.fromBytes(attrsMemorySegment.asSlice(LineAttribute.LAYOUT.byteSize() * i, LineAttribute.LAYOUT.byteSize()));
        }
        return new LineInfo(
                ((MemorySegment) MH_NAME.invokeExact(buffer, 0L)).toArray(ValueLayout.JAVA_BYTE),
                ((MemorySegment) MH_CONSUMER.invokeExact(buffer, 0L)).toArray(ValueLayout.JAVA_BYTE),
                (int) VH_OFFSET.get(buffer, 0L),
                numAttrs,
                (long) VH_FLAGS.get(buffer, 0L),
                attrs
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        var tmp = (MemorySegment) MH_NAME.invokeExact(buffer, 0L);
        for (int i = 0; i < name.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_BYTE, i, name[i]);
        }
        tmp = (MemorySegment) MH_CONSUMER.invokeExact(buffer, 0L);
        for (int i = 0; i < consumer.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_BYTE, i, consumer[i]);
        }
        VH_OFFSET.set(buffer, 0L, offset);
        VH_NUM_ATTRS.set(buffer, 0L,numAttrs);
        VH_FLAGS.set(buffer, 0L, flags);

        tmp = (MemorySegment) MH_ATTRS.invokeExact(buffer, 0L);
        for (int i = 0; i < numAttrs; i++) {
            attrs[i].toBytes(tmp.asSlice(LineAttribute.LAYOUT.byteSize() * i, LineAttribute.LAYOUT.byteSize()));
        }
        tmp = (MemorySegment) MH_PADDING.invokeExact(buffer, 0L);
        for (int i = 0; i < 4; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, 0);
        }
    }

    @Override
    public String toString() {
        return "LineInfo{" +
                "name=(" + new String(name).trim() + ")" + Arrays.toString(name) +
                ", consumer=(" + new String(consumer).trim() + ")" + Arrays.toString(consumer) +
                ", offset=" + offset +
                ", numAttrs=" + numAttrs +
                ", flags=" + flags +
                ", attrs=" + Arrays.toString(attrs) +
                '}';
    }
}

