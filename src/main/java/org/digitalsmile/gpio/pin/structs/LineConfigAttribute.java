package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * A configuration attribute associated with one or more of the requested lines.
 *
 * @param attr the configurable attribute
 * @param mask a bitmap identifying the lines to which the attribute applies, with each bit number corresponding to the index
 */
public record LineConfigAttribute(LineAttribute attr, long mask) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            LineAttribute.LAYOUT.withName("attr"),
            ValueLayout.JAVA_LONG.withName("mask")
    );
    private static final MethodHandle MH_ATTR = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("attr"));
    private static final VarHandle VH_MASK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("mask"));

    /**
     * Creates empty Line Config Attribute instance.
     *
     * @return empty Line Config Attribute instance
     */
    public static LineConfigAttribute createEmpty() {
        return new LineConfigAttribute(LineAttribute.createEmpty(), 0);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineConfigAttribute fromBytes(MemorySegment buffer) throws Throwable {
        var attrMemorySegment = (MemorySegment) MH_ATTR.invokeExact(buffer, 0L);
        var attr = LineAttribute.createEmpty().fromBytes(attrMemorySegment);
        return new LineConfigAttribute(
                attr,
                (long) VH_MASK.get(buffer, 0L)

        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        var attrMemorySegment = (MemorySegment) MH_ATTR.invokeExact(buffer, 0L);
        attr.toBytes(attrMemorySegment);
        VH_MASK.set(buffer, 0L, mask);
    }

    @Override
    public String toString() {
        return "LineConfigAttribute{" +
                "attr=" + attr +
                ", mask=" + mask +
                '}';
    }
}
