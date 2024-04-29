package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Values of GPIO lines.
 * @param bits  a bitmap containing the value of the lines, set to 1 for active and 0 for inactive
 * @param mask a bitmap identifying the lines to get or set, with each bit number corresponding to the index
 */
public record LineValues(long bits, long mask) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("bits"),
            ValueLayout.JAVA_LONG.withName("mask")
    );
    private static final VarHandle VH_BITS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bits"));
    private static final VarHandle VH_MASK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("mask"));


    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineValues fromBytes(MemorySegment buffer) throws Throwable {
        return new LineValues(
                (long) VH_BITS.get(buffer, 0L),
                (long) VH_MASK.get(buffer, 0L)
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_BITS.set(buffer, 0L, bits);
        VH_MASK.set(buffer, 0L, mask);
    }
}
