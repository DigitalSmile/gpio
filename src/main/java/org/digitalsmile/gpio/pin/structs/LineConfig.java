package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Configuration of GPIO Line
 *
 * @param flags    flags for the GPIO line. This is the default for all requested lines but may be overridden for particular lines using attributes
 * @param numAttrs the number of attributes
 * @param attrs    the configuration attributes associated with the requested lines.  Any attribute should only be associated with a particular line once.  If an attribute is associated with a line multiple times then the first occurrence (i.e. lowest index) has precedence.
 */
public record LineConfig(long flags, int numAttrs, LineConfigAttribute... attrs) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("flags"),
            ValueLayout.JAVA_INT.withName("num_attrs"),
            MemoryLayout.sequenceLayout(5, ValueLayout.JAVA_INT).withName("padding"),
            MemoryLayout.sequenceLayout(10, LineConfigAttribute.LAYOUT).withName("attrs")
    );
    private static final VarHandle VH_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("flags"));
    private static final VarHandle VH_NUM_ATTRS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("num_attrs"));
    private static final MethodHandle MH_PADDING = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("padding"));
    private static final MethodHandle MH_ATTRS = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("attrs"));

    /**
     * Creates empty Line Config instance.
     *
     * @return empty Line Config instance
     */
    public static LineConfig createEmpty() {
        return new LineConfig(0, 0);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineConfig fromBytes(MemorySegment buffer) throws Throwable {
        var numAttrs = (int) VH_NUM_ATTRS.get(buffer);
        var attrsMemorySegment = (MemorySegment) MH_ATTRS.invokeExact(buffer);
        var attrs = new LineConfigAttribute[numAttrs];
        for (int i = 0; i < numAttrs; i++) {
            var attr = LineConfigAttribute.createEmpty();
            attrs[i] = attr.fromBytes(attrsMemorySegment.asSlice(LineConfigAttribute.LAYOUT.byteSize() * i, LineConfigAttribute.LAYOUT.byteSize()));
        }
        return new LineConfig(
                (long) VH_FLAGS.get(buffer),
                numAttrs,
                attrs
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_FLAGS.set(buffer, flags);
        VH_NUM_ATTRS.set(buffer, numAttrs);
        var tmp = (MemorySegment) MH_PADDING.invokeExact(buffer);
        for (int i = 0; i < 5; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, 0);
        }
        tmp = (MemorySegment) MH_ATTRS.invokeExact(buffer);
        for (int i = 0; i < numAttrs; i++) {
            attrs[i].toBytes(tmp.asSlice(LineConfigAttribute.LAYOUT.byteSize() * i, LineConfigAttribute.LAYOUT.byteSize()));
        }
    }

    @Override
    public String toString() {
        return "LineConfig{" +
                "flags=" + flags +
                ", numAttrs=" + numAttrs +
                ", attrs=" + Arrays.toString(attrs) +
                '}';
    }
}
