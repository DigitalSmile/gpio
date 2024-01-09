package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

/**
 * Structure the represents GPIO data to send with ioctl.
 *
 * @param values - data to send through GPIO
 */
public record HandleDataStruct(byte[] values) implements NativeMemoryLayout {
    // see https://elixir.bootlin.com/linux/v6.7/source/include/uapi/linux/gpio.h#L449
    private static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(64, ValueLayout.JAVA_BYTE);

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public HandleDataStruct fromBytes(MemorySegment buffer) throws Throwable {
        return new HandleDataStruct(buffer.toArray(ValueLayout.JAVA_BYTE));
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
