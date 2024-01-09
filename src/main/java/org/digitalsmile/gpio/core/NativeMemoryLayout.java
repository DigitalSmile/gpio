package org.digitalsmile.gpio.core;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

/**
 * Interface that provides memory layout of class file for using with FFM API of recent Java versions.
 */
public interface NativeMemoryLayout {

    /**
     * Gets {@link MemoryLayout} from a class / object (structure).
     *
     * @return memory layout of class / object
     */
    MemoryLayout getMemoryLayout();

    /**
     * Converts {@link MemorySegment} buffer to a class / object structure.
     *
     * @param buffer - memory segment to convert from
     * @param <T>    type of converted class / object structure
     * @return new class / object structure from a given buffer
     * @throws Throwable unchecked exception
     */
    <T> T fromBytes(MemorySegment buffer) throws Throwable;

    /**
     * Converts a class / object structure into a {@link MemorySegment} buffer.
     *
     * @param buffer - buffer to be filled with class / object structure
     * @throws Throwable unchecked exception
     */
    void toBytes(MemorySegment buffer) throws Throwable;

}
