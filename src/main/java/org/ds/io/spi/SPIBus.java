package org.ds.io.spi;

import org.ds.io.core.IOCtl;
import org.ds.io.core.NativeCaller;

public class SPIBus {
    private static final String BASE_SPI_PATH = "/dev/spidev0.";

    private final String path;
    private SPIMode spiMode;
    private int clockFrequency;
    private int byteLength;
    private int bitOrdering;


    private final int spiFileDescriptor;
    public SPIBus(int busNumber, SPIMode spiMode, int clockFrequency, int byteLength, int bitOrdering) {
        this.path = BASE_SPI_PATH + busNumber;
        this.spiMode = spiMode;
        this.clockFrequency = clockFrequency;
        this.byteLength = byteLength;
        this.bitOrdering = bitOrdering;

        this.spiFileDescriptor = NativeCaller.open(path);

        init();
    }

    public SPIBus(int busNumber, SPIMode spiMode, int clockFrequency) {
        this(busNumber, spiMode, clockFrequency, 8, 0);
    }

    public String getPath() {
        return path;
    }

    public int getClockFrequency() {
        return clockFrequency;
    }

    public void setClockFrequency(int clockFrequency) {
        this.clockFrequency = clockFrequency;
        init();
    }

    public SPIMode getSPIMode() {
        return spiMode;
    }

    public void setSPIMode(SPIMode spiMode) {
        this.spiMode = spiMode;
        init();
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
        init();
    }

    public void setBitOrdering(int bitOrdering) {
        this.bitOrdering = bitOrdering;
        init();
    }

    public int getBitOrdering() {
        return bitOrdering;
    }

    public void init() {
        NativeCaller.ioctl(spiFileDescriptor, IOCtl.getSpiIocWrMode(), spiMode.getValue());
        NativeCaller.ioctl(spiFileDescriptor, IOCtl.getSpiIocWrLsbFirst(), bitOrdering);
        NativeCaller.ioctl(spiFileDescriptor, IOCtl.getSpiIocWrBitsPerWord(), byteLength);
        NativeCaller.ioctl(spiFileDescriptor, IOCtl.getSpiIocWrMaxSpeedHz(), clockFrequency);
    }

    public byte[] sendByteData(byte[] data, boolean immediateRead) {
        NativeCaller.write(spiFileDescriptor, data);
        return immediateRead ? NativeCaller.read(spiFileDescriptor, 1) : new byte[]{};
    }
}
