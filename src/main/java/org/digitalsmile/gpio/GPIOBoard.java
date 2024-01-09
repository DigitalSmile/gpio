package org.digitalsmile.gpio;

import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.digitalsmile.gpio.pin.Pin;
import org.digitalsmile.gpio.pin.attributes.Direction;
import org.digitalsmile.gpio.pin.structs.InfoStruct;
import org.digitalsmile.gpio.spi.SPIBus;
import org.digitalsmile.gpio.spi.SPIMode;

import java.io.IOException;

public class GPIOBoard {

    private static final String DEFAULT_GPIO_DEVICE = "/dev/gpiochip0";
    private static final String BASE_SPI_PATH = "/dev/spidev0.";

    private static InfoStruct infoStruct;


    private static void initialize(String deviceName) {
        if (infoStruct == null) {
            var fd = FileDescriptor.open(deviceName);
            infoStruct = IOCtl.call(fd, Command.getGpioGetChipInfoIoctl(), new InfoStruct(new byte[]{}, new byte[]{}, 0));
        }
    }

    public static Pin ofPin(String gpioDeviceName, int pinNumber, Direction direction) throws IOException {
        initialize(gpioDeviceName);
        return new Pin(gpioDeviceName, pinNumber, direction);
    }

    public static Pin ofPin(int pinNumber) throws IOException {
        return ofPin(DEFAULT_GPIO_DEVICE, pinNumber, Direction.OUTPUT);
    }

    public static Pin ofPin(int pinNumber, Direction direction) throws IOException {
        return ofPin(DEFAULT_GPIO_DEVICE, pinNumber, direction);
    }

    public static SPIBus ofSPI(String gpioDeviceName, String spiPath, int busNumber, SPIMode spiMode, int clockFrequency, int byteLength,
                               int bitOrdering) throws IOException {
        initialize(gpioDeviceName);
        return new SPIBus(spiPath, busNumber, spiMode, clockFrequency, byteLength, bitOrdering);
    }

    public static SPIBus ofSPI(int busNumber, SPIMode spiMode, int clockFrequency, int byteLength,
                               int bitOrdering) throws IOException {
        return ofSPI(DEFAULT_GPIO_DEVICE, BASE_SPI_PATH, busNumber, spiMode, clockFrequency, byteLength, bitOrdering);
    }

    public static SPIBus ofSPI(int busNumber, SPIMode spiMode, int clockFrequency) throws IOException {
        return ofSPI(DEFAULT_GPIO_DEVICE, BASE_SPI_PATH, busNumber, spiMode, clockFrequency, 8, 0);
    }

    @Override
    public String toString() {
        return infoStruct.toString();
    }
}
