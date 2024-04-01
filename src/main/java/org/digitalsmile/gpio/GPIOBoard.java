package org.digitalsmile.gpio;

import org.digitalsmile.gpio.core.exception.NativeException;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.digitalsmile.gpio.i2c.I2CBus;
import org.digitalsmile.gpio.pin.Pin;
import org.digitalsmile.gpio.pin.attributes.Direction;
import org.digitalsmile.gpio.pin.structs.InfoStruct;
import org.digitalsmile.gpio.spi.SPIBus;
import org.digitalsmile.gpio.spi.attributes.Mode;

/**
 * Class for creating abstractions over GPIO. It uses native FFM calls (such as open and ioctl) to operate with hardware.
 * Please, consider creating all interfaces through this general class.
 */
public final class GPIOBoard {

    private static final String DEFAULT_GPIO_DEVICE = "/dev/gpiochip0";
    private static final String BASE_SPI_PATH = "/dev/spidev0.";
    private static final String BASE_I2C_PATH = "/dev/i2c-";

    private static InfoStruct infoStruct;

    /**
     * Forbids creating an instance of this class.
     */
    private GPIOBoard() {
    }

    /**
     * Initializes the GPIO Board by given device name.
     *
     * @param deviceName given device name
     */
    private static void initialize(String deviceName) throws NativeException {
        if (infoStruct == null) {
            var fd = FileDescriptor.open(deviceName);
            infoStruct = IOCtl.call(fd, Command.getGpioGetChipInfoIoctl(), InfoStruct.createEmpty());
        }
    }

    /**
     * Creates GPIO Pin using GPIO device name, pin and direction.
     *
     * @param gpioDeviceName GPIO device name
     * @param pinNumber      pin
     * @param direction      direction
     * @return GPIO Pin instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static Pin ofPin(String gpioDeviceName, int pinNumber, Direction direction) throws NativeException {
        initialize(gpioDeviceName);
        return new Pin(gpioDeviceName, pinNumber, direction);
    }

    /**
     * Creates GPIO Pin using just pin. All other fields are defaults.
     *
     * @param pinNumber pin
     * @return GPIO Pin instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static Pin ofPin(int pinNumber) throws NativeException {
        return ofPin(DEFAULT_GPIO_DEVICE, pinNumber, Direction.OUTPUT);
    }

    /**
     * Creates GPIO Pin using just pin and direction. All other fields are defaults.
     *
     * @param pinNumber pin
     * @param direction direction
     * @return GPIO Pin instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static Pin ofPin(int pinNumber, Direction direction) throws NativeException {
        return ofPin(DEFAULT_GPIO_DEVICE, pinNumber, direction);
    }

    /**
     * Creates SPI Bus from given GPIO device name, path to spi bus, bus number, spi mode, clock frequency, length of byte and bit order.
     *
     * @param gpioDeviceName GPIO device name
     * @param spiPath        path to spi bus
     * @param busNumber      bus number
     * @param mode           spi mode
     * @param clockFrequency clock frequency
     * @param byteLength     length if byte
     * @param bitOrdering    bit order
     * @return SPI Bus instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static SPIBus ofSPI(String gpioDeviceName, String spiPath, int busNumber, Mode mode, int clockFrequency, int byteLength,
                               int bitOrdering) throws NativeException {
        initialize(gpioDeviceName);
        return new SPIBus(spiPath, busNumber, mode, clockFrequency, byteLength, bitOrdering);
    }

    /**
     * Creates SPI Bus from given bus number, spi mode, clock frequency, length of byte and bit order. All other fields are defaults.
     *
     * @param busNumber      bus number
     * @param mode           spi mode
     * @param clockFrequency clock frequency
     * @param byteLength     length if byte
     * @param bitOrdering    bit order
     * @return SPI Bus instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static SPIBus ofSPI(int busNumber, Mode mode, int clockFrequency, int byteLength,
                               int bitOrdering) throws NativeException {
        return ofSPI(DEFAULT_GPIO_DEVICE, BASE_SPI_PATH, busNumber, mode, clockFrequency, byteLength, bitOrdering);
    }

    /**
     * Creates SPI Bus from given bus number, spi mode, clock frequency. All other fields are defaults.
     *
     * @param busNumber      bus number
     * @param mode           spi mode
     * @param clockFrequency clock frequency
     * @return SPI Bus instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static SPIBus ofSPI(int busNumber, Mode mode, int clockFrequency) throws NativeException {
        return ofSPI(DEFAULT_GPIO_DEVICE, BASE_SPI_PATH, busNumber, mode, clockFrequency, 8, 0);
    }

    /**
     * Creates I2CBus from given device name and bus number.
     *
     * @param i2cDeviceName device name
     * @param busNumber     bus number
     * @return I2CBus instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static I2CBus ofI2C(String i2cDeviceName, int busNumber) throws NativeException {
        return new I2CBus(i2cDeviceName, busNumber);
    }

    /**
     * Creates I2CBus from given bus number with default device name.
     *
     * @param busNumber bus number
     * @return I2CBus instance
     * @throws NativeException if errors occurred during creating instance
     */
    public static I2CBus ofI2C(int busNumber) throws NativeException {
        return ofI2C(BASE_I2C_PATH, busNumber);
    }


    @Override
    public String toString() {
        return infoStruct.toString();
    }
}
