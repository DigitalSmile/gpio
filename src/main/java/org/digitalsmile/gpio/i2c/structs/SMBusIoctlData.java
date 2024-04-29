package org.digitalsmile.gpio.i2c.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Structure that represents SBBusIoctlData for communicating through ioctl.
 *
 * @param readWrite read or write byte
 * @param command   command to execute
 * @param size      size of data
 * @param data      the data to be sent
 */
public record SMBusIoctlData(byte readWrite, byte command, int size, SMBusData data) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BYTE.withName("read_write"),
            ValueLayout.JAVA_BYTE.withName("command"),
            MemoryLayout.paddingLayout(2),
            ValueLayout.JAVA_INT.withName("size"),
            ValueLayout.ADDRESS.withTargetLayout(SMBusData.LAYOUT).withName("data")
    );
    private static final VarHandle VH_READ_WRITE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("read_write"));
    private static final VarHandle VH_COMMAND = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("command"));
    private static final VarHandle VH_SIZE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("size"));
    private static final VarHandle MH_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));

    private static final Arena offHeap = Arena.ofAuto();

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SMBusIoctlData fromBytes(MemorySegment buffer) throws Throwable {
        var readWrite = (byte) VH_READ_WRITE.get(buffer, 0L);
        var command = (byte) VH_COMMAND.get(buffer, 0L);
        var size = (int) VH_SIZE.get(buffer, 0L);
        var tmp = (MemorySegment) MH_DATA.get(buffer, 0L);
        var data0 = data.fromBytes(tmp);
        return new SMBusIoctlData(readWrite, command, size, data0);
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_READ_WRITE.set(buffer, 0L, readWrite);
        VH_COMMAND.set(buffer, 0L, command);
        VH_SIZE.set(buffer, 0L, size);
        var smbusOffHeap = offHeap.allocate(SMBusData.LAYOUT);
        data.toBytes(smbusOffHeap);
        MH_DATA.set(buffer, 0L, smbusOffHeap);
    }

    @Override
    public String toString() {
        return "SMBusIoctlData{" +
                "readWrite=" + readWrite +
                ", command=" + command +
                ", size=" + size +
                ", data=" + data +
                '}';
    }
}
