package org.digitalsmile.gpio.core.ioctl;

/**
 * Commands to be provided for ioctl calls.
 */
public final class Command {

    /**
     * Forbids creating an instance of this class.
     */
    private Command() {
    }

    public static long getGpioGetChipInfoIoctl() {
        return Internals.GPIO_GET_CHIPINFO_IOCTL;
    }

    public static long getGpioGetLineInfoIoctl() {
        return Internals.GPIO_GET_LINEINFO_IOCTL;
    }

    public static long getSpiIocRdMode() {
        return Internals.SPI_IOC_RD_MODE;
    }

    public static long getSpiIocWrMode() {
        return Internals.SPI_IOC_WR_MODE;
    }

    public static long getSpiIocRdBitsPerWord() {
        return Internals.SPI_IOC_RD_BITS_PER_WORD;
    }

    public static long getSpiIocWrBitsPerWord() {
        return Internals.SPI_IOC_WR_BITS_PER_WORD;
    }

    public static long getSpiIocRdMaxSpeedHz() {
        return Internals.SPI_IOC_RD_MAX_SPEED_HZ;
    }

    public static long getSpiIocWrMaxSpeedHz() {
        return Internals.SPI_IOC_WR_MAX_SPEED_HZ;
    }

    public static long getSpiIocRdMode32() {
        return Internals.SPI_IOC_RD_MODE32;
    }

    public static long getSpiIocWrMode32() {
        return Internals.SPI_IOC_WR_MODE32;
    }

    public static long getSpiIocRdLsbFirst() {
        return Internals.SPI_IOC_RD_LSB_FIRST;
    }

    public static long getSpiIocWrLsbFirst() {
        return Internals.SPI_IOC_WR_LSB_FIRST;
    }

    public static long getGpioGetLineHandleIoctl() {
        return Internals.GPIO_GET_LINEHANDLE_IOCTL;
    }

    public static long getGpioGetLineeventIoctl() {
        return Internals.GPIO_GET_LINEEVENT_IOCTL;
    }

    public static long getGpioHandleGetLineValuesIoctl() {
        return Internals.GPIOHANDLE_GET_LINE_VALUES_IOCTL;
    }

    public static long getGpioHandleSetLineValuesIoctl() {
        return Internals.GPIOHANDLE_SET_LINE_VALUES_IOCTL;
    }
}
