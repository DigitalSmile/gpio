package org.ds.io.gpio.model;

import org.ds.io.core.NativeMemoryAccess;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

public record GPIOHandleData(byte[] values) implements NativeMemoryAccess {
    private static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(64, ValueLayout.JAVA_BYTE);

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @Override
    public GPIOHandleData fromBytes(MemorySegment buffer) throws Throwable {
        return new GPIOHandleData(buffer.toArray(ValueLayout.JAVA_BYTE));
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        for (int i = 0; i < values.length; i++) {
            buffer.setAtIndex(ValueLayout.JAVA_BYTE, i, values[i]);
        }
    }

    @Override
    public String toString() {
        return "GPIOHandleData{" +
                "values=" + Arrays.toString(values) +
                '}';
    }
}
