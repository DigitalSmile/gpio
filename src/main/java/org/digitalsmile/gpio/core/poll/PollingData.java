package org.digitalsmile.gpio.core.poll;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Polling data for linux poll. Contains single file descriptor, event bit mask for interested events and actual detected events.
 *
 * @param fd      file descriptor to be polled
 * @param events  interested events bit mask
 * @param revents detected events bit mask
 * @see <a href="https://elixir.bootlin.com/linux/latest/source/include/uapi/asm-generic/poll.h#L36">linux sources</a>
 */
public record PollingData(int fd, short events, short revents) implements NativeMemoryLayout {
    private static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("fd"),
            ValueLayout.JAVA_SHORT.withName("events"),
            ValueLayout.JAVA_SHORT.withName("revents")
    );
    private static final VarHandle FD_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fd"));
    private static final VarHandle EVENTS_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("events"));
    private static final VarHandle REVENTS_FLAGS = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("revents"));

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PollingData fromBytes(MemorySegment buffer) throws Throwable {
        return new PollingData(
                (int) FD_FLAGS.get(buffer, 0L),
                (short) EVENTS_FLAGS.get(buffer, 0L),
                (short) REVENTS_FLAGS.get(buffer, 0L)
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        FD_FLAGS.set(buffer, 0L, fd);
        EVENTS_FLAGS.set(buffer, 0L, events);
        REVENTS_FLAGS.set(buffer, 0L, revents);
    }

    @Override
    public String toString() {
        return "PollFd{" +
                "fd=" + fd +
                ", events=" + events +
                ", revents=" + revents +
                '}';
    }
}
