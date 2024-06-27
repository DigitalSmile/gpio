package org.digitalsmile.gpio.i2c;

import io.github.digitalsmile.annotation.function.NativeMemoryException;
import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.core.IntegerToHex;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.file.FileDescriptorNative;
import org.digitalsmile.gpio.core.file.FileFlag;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.Ioctl;
import org.digitalsmile.gpio.core.ioctl.IoctlNative;
import org.digitalsmile.gpio.core.poll.Poll;
import org.digitalsmile.gpio.core.poll.PollNative;
import org.digitalsmile.gpio.i2c.attributes.I2CFlag;
import org.digitalsmile.gpio.i2c.attributes.I2CFunctionality;
import org.digitalsmile.gpio.i2c.attributes.I2CStatus;
import org.digitalsmile.gpio.i2c.structs.SMBusData;
import org.digitalsmile.gpio.i2c.structs.SMBusIoctlData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for creating I2C / SMBus object. It uses native FFM calls (such as open and ioctl) to operate with hardware.
 * Instance of I2CBus can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
 * <p>
 * Since I2C and SMBus are usually mixed, we will guess the interface and use available.
 * Priority as follows:
 * 1) Raw I2C synchronous communication through files.
 * 2) SMBus synchronous communication with WORD or BLOCK through ioctl.
 * <p>
 * Before reading / writing, please select the deviceAddress. All device addresses can be found by scan method.
 */
public class I2CBus implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(I2CBus.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final Ioctl IOCTL = new IoctlNative();
    private static final FileDescriptor FILE = new FileDescriptorNative();
    private static final Poll POLL = new PollNative();

    private final String path;
    private final int i2cFileDescriptor;
    private final Map<I2CFunctionality, Boolean> functionalityMap = new HashMap<>();

    // selected device
    private int selectedAddress = -1;

    /**
     * Creates I2CBus object and gets functionalities.
     *
     * @param i2cPath   path to i2c device
     * @param busNumber bus number of i2c device
     * @throws NativeMemoryException if we cannot initialize the object
     */
    public I2CBus(String i2cPath, int busNumber) throws NativeMemoryException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new RuntimeException("Wrong call of constructor, I2CBus should be created by using GPIOBoard.ofI2C(...) methods.");
        }
        this.path = i2cPath + busNumber;

        logger.debug("{} - setting up I2CBus...", path);
        logger.debug("{} - opening device file.", path);
        this.i2cFileDescriptor = FILE.open(path, FileFlag.O_RDWR);
        logger.debug("{} - loading supported functionalities.", path);
        var i2cFunctions = IOCTL.call(i2cFileDescriptor, Command.getI2CFuncs(), 0);
        for (I2CFunctionality i2CFunctionality : I2CFunctionality.values()) {
            var supported = (i2cFunctions & i2CFunctionality.getValue()) != 0;
            functionalityMap.put(i2CFunctionality, supported);
            logger.trace("{} - functionality {}({}) is {}.", path, i2CFunctionality.name(), IntegerToHex.convert(i2CFunctionality.getValue()), supported ? "supported" : "not supported");
        }
        if (functionalityMap.get(I2CFunctionality.I2C_FUNC_I2C)) {
            logger.debug("{} - I2CBus will be using direct file mode for read/write operations.", path);
        } else if (functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_BYTE_DATA) ||
                functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_WORD_DATA) ||
                functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_I2C_BLOCK)) {
            logger.debug("{} - I2CBus will be using ioctl with SMBus mode for read/write operations.", path);
        } else {
            logger.error("{} - Cannot configure I2CBus!", path);
            for (Map.Entry<I2CFunctionality, Boolean> functionality : functionalityMap.entrySet()) {
                logger.error("{} - functionality {}({}) is {}.", path, functionality.getKey().name(),
                        IntegerToHex.convert(functionality.getKey().getValue()),
                        functionality.getValue() ? "supported" : "not supported");
            }
            throw new NativeMemoryException(path + " does not support any of read/write operations!");
        }
        logger.debug("{} - I2CBus configured.", path);
    }

    public Map<I2CFunctionality, Boolean> getFunctionalities() {
        return functionalityMap;
    }

    /**
     * Scans the i2c bus for any devices.
     *
     * @return map of all addresses with corresponding statuses (AVAILABLE, BUSY, NOT_AVAILABLE, UNKNOWN)
     */
    public Map<Integer, I2CStatus> scan() {
        logger.debug("{} - start scan of I2CBus for available devices...", path);
        Map<Integer, I2CStatus> addressStatusMap = new HashMap<>();
        for (int i = 0; i <= I2CFlag.MAX_7BIT_DEVICES; i++) {
            try {
                selectAddressInternal(i);
            } catch (NativeMemoryException e) {
                var error = e.getErrorCode();
                addressStatusMap.put(i, error == I2CFlag.EBUSY ? I2CStatus.BUSY : I2CStatus.UNKNOWN);
                if (error == I2CFlag.EBUSY) {
                    logger.debug("{} - Found busy device at address {}!", path, IntegerToHex.convert(i));
                }
            }

            try {
                readInternal(0x00);
                addressStatusMap.put(i, I2CStatus.AVAILABLE);
                logger.debug("{} - Found available device at address {}!", path, IntegerToHex.convert(i));
            } catch (NativeMemoryException e) {
                addressStatusMap.put(i, I2CStatus.NOT_AVAILABLE);
            }
            this.selectedAddress = -1;
        }
        logger.debug("{} - found {} devices!", path, addressStatusMap.entrySet()
                .stream().filter(e -> e.getValue().equals(I2CStatus.AVAILABLE) || e.getValue().equals(I2CStatus.BUSY)).count());
        return addressStatusMap;
    }

    /**
     * Selects the device address for communication.
     *
     * @param address device address on the bus
     * @throws NativeMemoryException if the address cannot be selected
     */
    public void selectAddress(int address) throws NativeMemoryException {
        logger.debug("{} - selecting address '{}'.", path, IntegerToHex.convert(address));
        selectAddressInternal(address);
    }

    /**
     * Selects 10BIT device address for communication.
     *
     * @param address        device address on the bus
     * @param tenBitsAddress true if the address is in 10 bits address map
     * @throws NativeMemoryException if the address cannot be selected
     */
    public void selectAddress(int address, boolean tenBitsAddress) throws NativeMemoryException {
        if (tenBitsAddress && functionalityMap.get(I2CFunctionality.I2C_FUNC_10BIT_ADDR)) {
            IOCTL.callByValue(i2cFileDescriptor, Command.getI2CTenBit(), 1);
        } else {
            throw new NativeMemoryException("Cannot set 10bit address, because device '" + path + "' does not support 10bit addressing extension.");
        }
        selectAddress(address);
    }

    /**
     * Writes the data byte into the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @param data            data byte to be written
     * @throws NativeMemoryException if address is not selected or there is issue while writing the data
     */
    public void write(int registerAddress, int data) throws NativeMemoryException {
        checkAddressSelected();
        logger.debug("{} - writing to '{}' with data '{}'.", path, IntegerToHex.convert(registerAddress), IntegerToHex.convert(data));
        writeInternal(registerAddress, data);
    }

    /**
     * Writes the data array into the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @param data            data array to be written
     * @throws NativeMemoryException if address is not selected or there is issue while writing the data
     */
    public void write(int registerAddress, byte[] data) throws NativeMemoryException {
        checkAddressSelected();
        if (data.length > I2CFlag.I2C_SMBUS_BLOCK_MAX) {
            throw new RuntimeException("The size of write block data must not be more than 32 bytes.");
        }
        logger.debug("{} - writing to '{}' with data '{}'.", path, IntegerToHex.convert(registerAddress), Arrays.toString(data));
        writeInternal(registerAddress, data);
    }

    /**
     * Reads the data byte from the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @return data byte read from register
     * @throws NativeMemoryException if address is not selected or there is issue while reading the data
     */
    public int read(int registerAddress) throws NativeMemoryException {
        checkAddressSelected();
        logger.debug("{} - reading from '{}'.", path, IntegerToHex.convert(registerAddress));
        return readInternal(registerAddress);
    }

    /**
     * Reads the data array from the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @param size            the size of the data tob read (should not be more than 32 bytes!)
     * @return data array read from register
     * @throws NativeMemoryException if address is not selected or there is issue while reading the data
     */
    public byte[] read(int registerAddress, int size) throws NativeMemoryException {
        checkAddressSelected();
        if (size > I2CFlag.I2C_SMBUS_BLOCK_MAX) {
            throw new RuntimeException("The size of read block data must not be more than 32 bytes.");
        }
        logger.debug("{} - reading from '{}' {} bytes.", path, IntegerToHex.convert(registerAddress), size);
        return readInternal(registerAddress, size);
    }

    /**
     * Internal method of selecting address.
     *
     * @param address address to be selected
     * @throws NativeMemoryException if the address cannot be selected
     */
    private void selectAddressInternal(int address) throws NativeMemoryException {
        IOCTL.callByValue(i2cFileDescriptor, Command.getI2CSlave(), address);
        this.selectedAddress = address;
    }

    /**
     * Internal method.
     * Writes the data byte into the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @param data            data byte to be written
     * @throws NativeMemoryException if address is not selected or there is issue while writing the data
     */
    private void writeInternal(int registerAddress, int data) throws NativeMemoryException {
        if (functionalityMap.get(I2CFunctionality.I2C_FUNC_I2C)) {
            var buffer = new byte[2];
            buffer[0] = (byte) registerAddress;
            buffer[1] = (byte) data;
            FILE.write(i2cFileDescriptor, buffer);
        } else if (functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_WRITE_I2C_BLOCK)) {
            var smbusData = new SMBusIoctlData(I2CFlag.I2C_SMBUS_WRITE, (byte) registerAddress, I2CFlag.I2C_SMBUS_WORD_DATA,
                    new SMBusData((byte) data, (short) 0, new byte[]{}));
            IOCTL.call(i2cFileDescriptor, Command.getI2CSMBus(), smbusData);
        } else {
            throw new NativeMemoryException("No available write method is supported!");
        }
    }

    /**
     * Internal method.
     * Writes the data array into the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @param data            data array to be written
     * @throws NativeMemoryException if address is not selected or there is issue while writing the data
     */
    private void writeInternal(int registerAddress, byte[] data) throws NativeMemoryException {
        if (functionalityMap.get(I2CFunctionality.I2C_FUNC_I2C)) {
            var buffer = new byte[data.length + 1];
            buffer[0] = (byte) registerAddress;
            System.arraycopy(data, 0, buffer, 1, data.length);
            FILE.write(i2cFileDescriptor, buffer);
        } else if (functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_WRITE_I2C_BLOCK)) {
            var buffer = new byte[data.length + 1];
            buffer[0] = (byte) data.length;
            System.arraycopy(data, 0, buffer, 1, data.length);
            var smbusData = new SMBusIoctlData(I2CFlag.I2C_SMBUS_WRITE, (byte) registerAddress, I2CFlag.I2C_SMBUS_I2C_BLOCK_DATA,
                    new SMBusData((byte) 0, (short) 0, buffer));
            IOCTL.call(i2cFileDescriptor, Command.getI2CSMBus(), smbusData);
        } else {
            throw new NativeMemoryException("No available write method is supported!");
        }
    }

    /**
     * Internal method.
     * Reads the data byte from the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @return data byte read from register
     * @throws NativeMemoryException if address is not selected or there is issue while reading the data
     */
    private int readInternal(int registerAddress) throws NativeMemoryException {
        var result = 0;
        if (functionalityMap.get(I2CFunctionality.I2C_FUNC_I2C)) {
            FILE.write(i2cFileDescriptor, new byte[]{(byte) registerAddress});
            var buffer = FILE.read(i2cFileDescriptor, new byte[4], 4);
            result = ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getInt();
        } else if (functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_READ_WORD_DATA)) {
            var smbusData = new SMBusIoctlData(I2CFlag.I2C_SMBUS_READ, (byte) registerAddress, I2CFlag.I2C_SMBUS_WORD_DATA,
                    SMBusData.createEmpty());
            var tempResult = IOCTL.call(i2cFileDescriptor, Command.getI2CSMBus(), smbusData);
            result = tempResult.data()._byte();
        } else {
            throw new NativeMemoryException("No available read method is supported!");
        }
        return result;
    }

    /**
     * Internal method.
     * Reads the data array from the register address of device selected previously.
     *
     * @param registerAddress register address of selected device
     * @param size            the size of the data tob read (should not be more than 32 bytes!)
     * @return data array read from register
     * @throws NativeMemoryException if address is not selected or there is issue while reading the data
     */
    private byte[] readInternal(int registerAddress, int size) throws NativeMemoryException {
        var result = new byte[]{};
        if (functionalityMap.get(I2CFunctionality.I2C_FUNC_I2C)) {
            FILE.write(i2cFileDescriptor, new byte[]{(byte) registerAddress});
            result = FILE.read(i2cFileDescriptor, new byte[size], size);
        } else if (functionalityMap.get(I2CFunctionality.I2C_FUNC_SMBUS_READ_I2C_BLOCK)) {
            var buffer = new byte[size + 1];
            buffer[0] = (byte) size;
            var smbusData = new SMBusIoctlData(I2CFlag.I2C_SMBUS_READ, (byte) registerAddress, I2CFlag.I2C_SMBUS_I2C_BLOCK_DATA,
                    new SMBusData((byte) 0, (short) 0, buffer));
            var tempResult = IOCTL.call(i2cFileDescriptor, Command.getI2CSMBus(), smbusData);
            result = tempResult.data().block();
        } else {
            throw new NativeMemoryException("No available read method is supported!");
        }
        return result;
    }

    /**
     * Checks if the address is selected.
     */
    private void checkAddressSelected() {
        if (selectedAddress == -1) {
            throw new RuntimeException("Address is not selected, please use I2CBus.selectAddress(...) method first.");
        }
    }

    @Override
    public void close() throws NativeMemoryException {
        FILE.close(i2cFileDescriptor);
    }
}
