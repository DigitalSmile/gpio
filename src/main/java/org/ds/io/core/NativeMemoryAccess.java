package org.ds.io.core;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public interface NativeMemoryAccess {

    MemoryLayout getMemoryLayout();
    <T> T fromBytes(MemorySegment buffer) throws Throwable;
    void toBytes(MemorySegment buffer) throws Throwable;

}
