package org.digitalsmile.gpio.spi;

import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class SPIBus {
    private static final Logger logger = LoggerFactory.getLogger(SPIBus.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final String path;
    private SPIMode spiMode;
    private int clockFrequency;
    private int byteLength;
    private int bitOrdering;

    private final int spiFileDescriptor;
    private boolean closed = false;

    public SPIBus(String spiPath, int busNumber, SPIMode spiMode, int clockFrequency, int byteLength, int bitOrdering) throws IOException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new IOException("Wrong call of constructor, SPIBus should be created by using GPIOBoard.ofSPI(...) methods.");
        }
        this.path = spiPath + busNumber;
        this.spiMode = spiMode;
        this.clockFrequency = clockFrequency;
        this.byteLength = byteLength;
        this.bitOrdering = bitOrdering;

        logger.info("{} - setting up SPIBus...", path);
        logger.debug("{} - opening device file", path);
        this.spiFileDescriptor = FileDescriptor.open(path);

        init();
    }

    public String getPath() {
        return path;
    }

    public int getClockFrequency() {
        return clockFrequency;
    }

    public void setClockFrequency(int clockFrequency) throws IOException {
        this.clockFrequency = clockFrequency;
        logger.info("{} - clock frequency changed, reinitialize SPIBus...", path);
        init();
    }

    public SPIMode getSPIMode() {
        return spiMode;
    }

    public void setSPIMode(SPIMode spiMode) throws IOException {
        this.spiMode = spiMode;
        logger.info("{} - spi mode changed, reinitialize SPIBus...", path);
        init();
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) throws IOException {
        this.byteLength = byteLength;
        logger.info("{} - byte length changed, reinitialize SPIBus...", path);
        init();
    }

    public void setBitOrdering(int bitOrdering) throws IOException {
        this.bitOrdering = bitOrdering;
        logger.info("{} - bit ordering changed, reinitialize SPIBus...", path);
        init();
    }

    public int getBitOrdering() {
        return bitOrdering;
    }

    public void init() throws IOException {
        checkClosed();
        logger.debug("{} - setting SPI Mode to {}.", path, spiMode);
        IOCtl.call(spiFileDescriptor, Command.getSpiIocWrMode(), spiMode.getValue());
        logger.debug("{} - setting Bit Ordering to {}.", path, bitOrdering);
        IOCtl.call(spiFileDescriptor, Command.getSpiIocWrLsbFirst(), bitOrdering);
        logger.debug("{} - setting Byte Length to {}.", path, byteLength);
        IOCtl.call(spiFileDescriptor, Command.getSpiIocWrBitsPerWord(), byteLength);
        logger.debug("{} - setting Clock Frequency to {}.", path, clockFrequency);
        IOCtl.call(spiFileDescriptor, Command.getSpiIocWrMaxSpeedHz(), clockFrequency);
    }

    public byte[] sendByteData(byte[] data, boolean immediateRead) throws IOException {
        checkClosed();
        logger.trace("{} - writing data {}.", path, data);
        FileDescriptor.write(spiFileDescriptor, data);
        var read = immediateRead ? FileDescriptor.read(spiFileDescriptor, 1) : new byte[]{};
        if (immediateRead) {
            logger.trace("{} - immediate read data {}.", path, read);
        }
        return read;
    }

    public void close() {
        logger.info("{} - closing SPIBus.", path);
        FileDescriptor.close(spiFileDescriptor);
        this.closed = true;
        logger.info("{} - SPIBus is closed. Recreate the SPIBus object to reuse.", path);
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("SPI bus  " + path + " is closed");
        }
    }

    @Override
    public String toString() {
        return "SPIBus{" +
                "path='" + path + '\'' +
                ", spiMode=" + spiMode +
                ", clockFrequency=" + clockFrequency +
                ", byteLength=" + byteLength +
                ", bitOrdering=" + bitOrdering +
                ", closed=" + closed +
                '}';
    }
}
