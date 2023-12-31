package org.ds.io.gpio;

import org.ds.io.core.IOCtl;
import org.ds.io.core.NativeCaller;
import org.ds.io.gpio.model.GPIOInfo;

import java.io.IOException;

public class GPIOBoard {

    private static final String DEFAULT_GPIO_DEVICE = "/dev/gpiochip0";

    private static GPIOInfo gpioInfo;

    private void initialize() {

    }

    public static GPIOPin ofPin(int pinNumber) throws IOException {
        return ofPin(pinNumber, GPIODirection.OUTPUT);
    }

    public static GPIOPin ofPin(int pinNumber, GPIODirection direction) throws IOException {
        if (gpioInfo == null) {
            var fd = NativeCaller.open(DEFAULT_GPIO_DEVICE);
            gpioInfo = NativeCaller.ioctl(fd, IOCtl.getGpioGetChipInfoIoctl(), new GPIOInfo(new byte[]{}, new byte[]{}, 0));
        }
        return new GPIOPin(DEFAULT_GPIO_DEVICE, pinNumber, direction);
    }
}
