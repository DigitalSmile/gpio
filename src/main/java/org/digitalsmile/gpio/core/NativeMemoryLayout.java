package org.digitalsmile.gpio.core;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public interface NativeMemoryLayout {

    MemoryLayout getMemoryLayout();

    <T> T fromBytes(MemorySegment buffer) throws Throwable;

    void toBytes(MemorySegment buffer) throws Throwable;

}
