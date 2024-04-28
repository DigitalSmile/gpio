package org.digitalsmile.gpio;

import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.digitalsmile.gpio.i2c.I2CBus;
import org.digitalsmile.gpio.pwm.PWMBus;
import org.digitalsmile.gpio.pin.Pin;
import org.digitalsmile.gpio.pin.attributes.PinDirection;
import org.digitalsmile.gpio.pin.structs.ChipInfo;
import org.digitalsmile.gpio.spi.SPIBus;
import org.digitalsmile.gpio.spi.attributes.SPIMode;

/**
 * Class for creating abstractions over GPIO. It uses native FFM calls (such as open and ioctl) to operate with hardware.
 * Please, consider creating all interfaces through this general class.
 */
public final class GPIOBoard {

    private static final String DEFAULT_GPIO_DEVICE = "/dev/gpiochip0";
    private static final String BASE_SPI_PATH = "/dev/spidev0.";
    private static final String BASE_I2C_PATH = "/dev/i2c-";

    private static ChipInfo chipInfo;

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
    private static void initialize(String deviceName) throws NativeMemoryException {
        if (chipInfo == null) {
            var gpioFd = FileDescriptor.open(deviceName);
            chipInfo = IOCtl.call(gpioFd, Command.getGpioGetChipInfoIoctl(), ChipInfo.createEmpty());
        }
    }

    /**
     * Creates GPIO Pin using GPIO device name, pin and direction.
     *
     * @param gpioDeviceName GPIO device name
     * @param pinNumber      pin
     * @param pinDirection      direction
     * @return GPIO Pin instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static Pin ofPin(String gpioDeviceName, int pinNumber, PinDirection pinDirection) throws NativeMemoryException {
        initialize(gpioDeviceName);
        return new Pin(gpioDeviceName, pinNumber, pinDirection);
    }

    /**
     * Creates GPIO Pin using just pin. All other fields are defaults.
     *
     * @param pinNumber pin
     * @return GPIO Pin instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static Pin ofPin(int pinNumber) throws NativeMemoryException {
        return ofPin(DEFAULT_GPIO_DEVICE, pinNumber, PinDirection.OUTPUT);
    }

    /**
     * Creates GPIO Pin using just pin and direction. All other fields are defaults.
     *
     * @param pinNumber pin
     * @param pinDirection direction
     * @return GPIO Pin instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static Pin ofPin(int pinNumber, PinDirection pinDirection) throws NativeMemoryException {
        return ofPin(DEFAULT_GPIO_DEVICE, pinNumber, pinDirection);
    }

    /**
     * Creates SPI Bus from given GPIO device name, path to spi bus, bus number, spi mode, clock frequency, length of byte and bit order.
     *
     * @param gpioDeviceName GPIO device name
     * @param spiPath        path to spi bus
     * @param busNumber      bus number
     * @param SPIMode           spi mode
     * @param clockFrequency clock frequency
     * @param byteLength     length if byte
     * @param bitOrdering    bit order
     * @return SPI Bus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static SPIBus ofSPI(String gpioDeviceName, String spiPath, int busNumber, SPIMode SPIMode, int clockFrequency, int byteLength,
                               int bitOrdering) throws NativeMemoryException {
        initialize(gpioDeviceName);
        return new SPIBus(spiPath, busNumber, SPIMode, clockFrequency, byteLength, bitOrdering);
    }

    /**
     * Creates SPI Bus from given bus number, spi mode, clock frequency, length of byte and bit order. All other fields are defaults.
     *
     * @param busNumber      bus number
     * @param SPIMode           spi mode
     * @param clockFrequency clock frequency
     * @param byteLength     length if byte
     * @param bitOrdering    bit order
     * @return SPI Bus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static SPIBus ofSPI(int busNumber, SPIMode SPIMode, int clockFrequency, int byteLength,
                               int bitOrdering) throws NativeMemoryException {
        return ofSPI(DEFAULT_GPIO_DEVICE, BASE_SPI_PATH, busNumber, SPIMode, clockFrequency, byteLength, bitOrdering);
    }

    /**
     * Creates SPI Bus from given bus number, spi mode, clock frequency. All other fields are defaults.
     *
     * @param busNumber      bus number
     * @param SPIMode           spi mode
     * @param clockFrequency clock frequency
     * @return SPI Bus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static SPIBus ofSPI(int busNumber, SPIMode SPIMode, int clockFrequency) throws NativeMemoryException {
        return ofSPI(DEFAULT_GPIO_DEVICE, BASE_SPI_PATH, busNumber, SPIMode, clockFrequency, 8, 0);
    }

    /**
     * Creates I2CBus from given device name and bus number.
     *
     * @param i2cDeviceName device name
     * @param busNumber     bus number
     * @return I2CBus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static I2CBus ofI2C(String i2cDeviceName, int busNumber) throws NativeMemoryException {
        return new I2CBus(i2cDeviceName, busNumber);
    }

    /**
     * Creates I2CBus from given bus number with default device name.
     *
     * @param busNumber bus number
     * @return I2CBus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static I2CBus ofI2C(int busNumber) throws NativeMemoryException {
        return ofI2C(BASE_I2C_PATH, busNumber);
    }

    /**
     * Creates hardware PWMBus from given PWM chip number and PWM bus number.
     * @param pwmChipNumber pwm chip number
     * @param pwmBusNumber pwm bus number
     * @return hardware PWMBus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static PWMBus ofPWMBus(int pwmChipNumber, int pwmBusNumber) throws NativeMemoryException {
        return new PWMBus(pwmChipNumber, pwmBusNumber);
    }

    /**
     * Creates hardware PWMBus from given PWM bus number and default PWM chip number.
     * @param pwmBusNumber pwm bus number
     * @return hardware PWMBus instance
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public static PWMBus ofPWMBus(int pwmBusNumber) throws NativeMemoryException {
        return new PWMBus(0, pwmBusNumber);
    }

    @Override
    public String toString() {
        return chipInfo.toString();
    }
}
