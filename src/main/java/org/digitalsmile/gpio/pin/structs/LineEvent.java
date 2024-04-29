package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * The actual event being pushed to userspace.
 * NOTE: all fields are unaligned, because we have to read events from file descriptor to raw byte buffer. There is no guarantee to have alignment in that case, so parsing is manual.
 *
 * @param timestampNs best estimate of time of event occurrence, in nanoseconds
 * @param id          event identifier with value
 * @param offset      the offset of the line that triggered the event
 * @param seqNo       the sequence number for this event in the sequence of events for all the lines in this line request
 * @param lineSeqNo   the sequence number for this event in the sequence of events on this particular line
 */
public record LineEvent(long timestampNs, int id, int offset, int seqNo, int lineSeqNo) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG_UNALIGNED.withName("timestampNs"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("id"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("offset"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("seqNo"),
            ValueLayout.JAVA_INT_UNALIGNED.withName("lineSeqNo"),
            MemoryLayout.sequenceLayout(6, ValueLayout.JAVA_INT_UNALIGNED).withName("padding")
    );

    private static final VarHandle VH_TIMESTAMP_NS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("timestampNs"));
    private static final VarHandle VH_ID = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("id"));
    private static final VarHandle VH_OFFSET = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("offset"));
    private static final VarHandle VH_SEQ_NO = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("seqNo"));
    private static final VarHandle VH_LINE_SEQ_NO = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("lineSeqNo"));
    private static final MethodHandle MH_PADDING = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("padding"));

    /**
     * Creates empty Line Event instance.
     *
     * @return empty Line Event instance
     */
    public static LineEvent createEmpty() {
        return new LineEvent(0, 0, 0, 0, 0);
    }


    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineEvent fromBytes(MemorySegment buffer) throws Throwable {
        return new LineEvent(
                (long) VH_TIMESTAMP_NS.get(buffer, 0L),
                (int) VH_ID.get(buffer, 0L),
                (int) VH_OFFSET.get(buffer, 0L),
                (int) VH_SEQ_NO.get(buffer, 0L),
                (int) VH_LINE_SEQ_NO.get(buffer, 0L)
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_TIMESTAMP_NS.set(buffer, 0L, timestampNs);
        VH_ID.set(buffer, 0L, id);
        VH_OFFSET.set(buffer, 0L, offset);
        VH_SEQ_NO.set(buffer, 0L, seqNo);
        VH_LINE_SEQ_NO.set(buffer, 0L, lineSeqNo);

        var tmp = (MemorySegment) MH_PADDING.invokeExact(buffer, 0L);
        for (int i = 0; i < 5; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_INT, i, 0);
        }
    }
}
