package org.digitalsmile.gpio.i2c.structs;

import org.digitalsmile.gpio.core.NativeMemoryLayout;
import org.digitalsmile.gpio.i2c.attributes.I2CFlag;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Structure that represents SMBusData package to send with ioctl call.
 *
 * @param _byte data byte
 * @param word  data word
 * @param block data block
 */
public record SMBusData(byte _byte, byte word, byte[] block) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BYTE.withName("byte"),
            ValueLayout.JAVA_BYTE.withName("word"),
            MemoryLayout.sequenceLayout(I2CFlag.I2C_SMBUS_BLOCK_MAX + 2, ValueLayout.JAVA_BYTE).withName("block")
    );
    private static final VarHandle VH_BYTE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("byte"));
    private static final VarHandle VH_WORD = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("word"));
    private static final MethodHandle MH_BLOCK = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement("block"));

    /**
     * Helper method to create empty object.
     *
     * @param _byte byte data
     * @return object instance
     */
    public static SMBusData createEmptyWithByte(byte _byte) {
        return new SMBusData(_byte, (byte) 0, new byte[]{});
    }

    /**
     * Helper method to create empty object.
     *
     * @param word word data
     * @return object instance
     */
    public static SMBusData createEmptyWithWord(byte word) {
        return new SMBusData((byte) 0, word, new byte[]{});
    }

    /**
     * Helper method to create empty object.
     *
     * @param block block data
     * @return object instance
     */
    public static SMBusData createEmptyWithBlock(byte[] block) {
        return new SMBusData((byte) 0, (byte) 0, block);
    }

    /**
     * Helper method to create empty object.
     *
     * @return empty object instance
     */
    public static SMBusData createEmpty() {
        return new SMBusData((byte) 0, (byte) 0, new byte[]{});
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SMBusData fromBytes(MemorySegment buffer) throws Throwable {
        var _byte = (byte) VH_BYTE.get(buffer);
        var word = (byte) VH_WORD.get(buffer);
        var block = ((MemorySegment) MH_BLOCK.invokeExact(buffer)).toArray(ValueLayout.JAVA_BYTE);
        return new SMBusData(_byte, word, block);
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_BYTE.set(buffer, _byte);
        VH_WORD.set(buffer, word);
        var tmp = ((MemorySegment) MH_BLOCK.invokeExact(buffer));
        for (int i = 0; i < block.length; i++) {
            tmp.setAtIndex(ValueLayout.JAVA_BYTE, i, block[i]);
        }
    }

    @Override
    public String toString() {
        return "SMBusData{" +
                "_byte=" + _byte +
                ", word=" + word +
                ", block=" + Arrays.toString(block) +
                '}';
    }
}
