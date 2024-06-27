package org.digitalsmile.gpio.spi;

import io.github.digitalsmile.annotation.function.NativeMemoryException;
import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.file.FileDescriptorNative;
import org.digitalsmile.gpio.core.file.FileFlag;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.Ioctl;
import org.digitalsmile.gpio.core.ioctl.IoctlNative;
import org.digitalsmile.gpio.spi.attributes.SPIMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for creating GPIO SPIBus object. It uses native FFM calls (such as open and ioctl) to operate with hardware.
 * Instance of SPIBus can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
 */
public final class SPIBus {
    private static final Logger logger = LoggerFactory.getLogger(SPIBus.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final Ioctl IOCTL = new IoctlNative();
    private static final FileDescriptor FILE = new FileDescriptorNative();

    private final String path;
    private SPIMode SPIMode;
    private int clockFrequency;
    private int byteLength;
    private int bitOrdering;

    private final int spiFileDescriptor;
    private boolean closed = false;

    /**
     * Constructs SPIBus object with given path, bus number, SPI mode, clock frequency, length of byte and bit order.
     * Instance of SPIBus can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
     *
     * @param spiPath        path to SPI bus device
     * @param busNumber      bus number
     * @param SPIMode        SPI mode to be configured
     * @param clockFrequency desired clock frequency of SPI bus
     * @param byteLength     byte length to be configured (usually 8)
     * @param bitOrdering    bit ordering in the byte
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public SPIBus(String spiPath, int busNumber, SPIMode SPIMode, int clockFrequency, int byteLength, int bitOrdering) throws NativeMemoryException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new RuntimeException("Wrong call of constructor, SPIBus should be created by using GPIOBoard.ofSPI(...) methods.");
        }
        this.path = spiPath + busNumber;
        this.SPIMode = SPIMode;
        this.clockFrequency = clockFrequency;
        this.byteLength = byteLength;
        this.bitOrdering = bitOrdering;

        logger.info("{} - setting up SPIBus...", path);
        logger.debug("{} - opening device file.", path);
        this.spiFileDescriptor = FILE.open(path, FileFlag.O_RDWR);

        init();
        logger.info("{} - SPIBus configured.", path);
    }

    /**
     * Gets the path to SPI bus device.
     *
     * @return path to SPI bus device
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets clock frequency of SPI bus,
     *
     * @return clock frequency of SPI bus,
     */
    public int getClockFrequency() {
        return clockFrequency;
    }

    /**
     * Sets the new clock frequency of SPI Bus and reinitialize the device.
     *
     * @param clockFrequency new clock frequency to SPI Bus
     * @throws NativeMemoryException if errors occurred during reinitializing
     */
    public void setClockFrequency(int clockFrequency) throws NativeMemoryException {
        this.clockFrequency = clockFrequency;
        logger.info("{} - clock frequency changed, reinitialize SPIBus...", path);
        init();
    }

    /**
     * Gets the SPI Bus mode.
     *
     * @return the SPI Bus mode
     */
    public SPIMode getSPIMode() {
        return SPIMode;
    }

    /**
     * Sets the new SPI mode of SPI Bus and reinitialize the device.
     *
     * @param SPIMode new SPI mode
     * @throws NativeMemoryException if errors occurred during reinitializing
     */
    public void setSPIMode(SPIMode SPIMode) throws NativeMemoryException {
        this.SPIMode = SPIMode;
        logger.info("{} - spi mode changed, reinitialize SPIBus...", path);
        init();
    }

    /**
     * Gets the length of byte (alignment).
     *
     * @return the length of byte
     */
    public int getByteLength() {
        return byteLength;
    }

    /**
     * Sets the new byte length of SPI Bus and reinitialize the device.
     *
     * @param byteLength new byte length
     * @throws NativeMemoryException if errors occurred during reinitializing
     */
    public void setByteLength(int byteLength) throws NativeMemoryException {
        this.byteLength = byteLength;
        logger.info("{} - byte length changed, reinitialize SPIBus...", path);
        init();
    }

    /**
     * Gets bit ordering.
     *
     * @return bit ordering
     */
    public int getBitOrdering() {
        return bitOrdering;
    }

    /**
     * Sets the new bit ordering of SPI Bus and reinitialize the device.
     *
     * @param bitOrdering new bit ordering
     * @throws NativeMemoryException if errors occurred during reinitializing
     */
    public void setBitOrdering(int bitOrdering) throws NativeMemoryException {
        this.bitOrdering = bitOrdering;
        logger.info("{} - bit ordering changed, reinitialize SPIBus...", path);
        init();
    }

    /**
     * Initialize the SPI Bus.
     *
     * @throws NativeMemoryException if errors occurred during initialization
     */
    public void init() throws NativeMemoryException {
        checkClosed();
        logger.debug("{} - setting SPI Mode to {}.", path, SPIMode);
        IOCTL.call(spiFileDescriptor, Command.getSpiIocWrMode(), SPIMode.getValue());
        logger.debug("{} - setting Bit Ordering to {}.", path, bitOrdering);
        IOCTL.call(spiFileDescriptor, Command.getSpiIocWrLsbFirst(), bitOrdering);
        logger.debug("{} - setting Byte Length to {}.", path, byteLength);
        IOCTL.call(spiFileDescriptor, Command.getSpiIocWrBitsPerWord(), byteLength);
        logger.debug("{} - setting Clock Frequency to {}.", path, clockFrequency);
        IOCTL.call(spiFileDescriptor, Command.getSpiIocWrMaxSpeedHz(), clockFrequency);
    }

    /**
     * Sends the byte into SPI Bus. Can immediately read from bus or skip the returned value.
     *
     * @param data          data to be sent to bus
     * @param immediateRead indicates if we should immediately read from bus after writing
     * @return byte array with data or zero length byte array (if no immediate read)
     * @throws NativeMemoryException if error occurred during writing the SPI Bus
     */
    public byte[] sendByteData(byte[] data, boolean immediateRead) throws NativeMemoryException {
        checkClosed();
        logger.trace("{} - writing data {}.", path, data);
        FILE.write(spiFileDescriptor, data);
        var read = immediateRead ? FILE.read(spiFileDescriptor, new byte[1], 1) : new byte[]{};
        if (immediateRead) {
            logger.trace("{} - immediate read data {}.", path, read);
        }
        return read;
    }

    /**
     * Closes the SPI Bus. Object must be recreated if used after closing.
     *
     * @throws NativeMemoryException if error occurred during closing the SPI Bus
     */
    public void close() throws NativeMemoryException {
        logger.info("{} - closing SPIBus.", path);
        FILE.close(spiFileDescriptor);
        this.closed = true;
        logger.info("{} - SPIBus is closed. Recreate the SPIBus object to reuse.", path);
    }

    /**
     * Checks if SPI Bus is closed.
     */
    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("SPI bus  " + path + " is closed");
        }
    }

    @Override
    public String toString() {
        return "SPIBus{" +
                "path='" + path + '\'' +
                ", spiMode=" + SPIMode +
                ", clockFrequency=" + clockFrequency +
                ", byteLength=" + byteLength +
                ", bitOrdering=" + bitOrdering +
                ", closed=" + closed +
                '}';
    }
}
