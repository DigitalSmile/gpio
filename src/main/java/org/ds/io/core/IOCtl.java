package org.ds.io.core;

public class IOCtl {
    private static final long SPI_IOC_RD_MODE;
    private static final long SPI_IOC_WR_MODE;
    private static final long SPI_IOC_RD_BITS_PER_WORD;
    private static final long SPI_IOC_WR_BITS_PER_WORD;

    private static final long SPI_IOC_RD_LSB_FIRST;
    private static final long SPI_IOC_WR_LSB_FIRST;

    private static final long SPI_IOC_RD_MAX_SPEED_HZ;
    private static final long SPI_IOC_WR_MAX_SPEED_HZ;
    private static final long SPI_IOC_RD_MODE32;
    private static final long SPI_IOC_WR_MODE32;

    private static int _SPI_IOC_TRANSFER_SIZE = -1;

    private static final long GPIO_GET_CHIPINFO_IOCTL;
    private static final long GPIO_GET_LINEINFO_IOCTL;
    private static final long GPIO_GET_LINEHANDLE_IOCTL;
    private static final long GPIO_GET_LINEEVENT_IOCTL;

    private static final long GPIOHANDLE_GET_LINE_VALUES_IOCTL;
    private static final long GPIOHANDLE_SET_LINE_VALUES_IOCTL;

    private final static long SPI_IOC_MAGIC = 'k';
    private final static long _IOC_NRBITS = 8;
    private final static long _IOC_TYPEBITS = 8;
    private final static long _IOC_SIZEBITS = 14;
    private final static long _IOC_DIRBITS = 2;
    private final static long _IOC_NRSHIFT = 0;
    private final static long _IOC_NONE = 0;
    private final static long _IOC_READ = 2;
    private final static long _IOC_WRITE = 1;
    private final static int GPIOHANDLES_MAX = 64;

    public static long getGpioGetChipInfoIoctl() {
        return GPIO_GET_CHIPINFO_IOCTL;
    }

    public static long getGpioGetLineInfoIoctl() {
        return GPIO_GET_LINEINFO_IOCTL;
    }

    public static long getSpiIocRdMode() {
        return SPI_IOC_RD_MODE;
    }

    public static long getSpiIocWrMode() {
        return SPI_IOC_WR_MODE;
    }

    public static long getSpiIocRdBitsPerWord() {
        return SPI_IOC_RD_BITS_PER_WORD;
    }

    public static long getSpiIocWrBitsPerWord() {
        return SPI_IOC_WR_BITS_PER_WORD;
    }

    public static long getSpiIocRdMaxSpeedHz() {
        return SPI_IOC_RD_MAX_SPEED_HZ;
    }

    public static long getSpiIocWrMaxSpeedHz() {
        return SPI_IOC_WR_MAX_SPEED_HZ;
    }

    public static long getSpiIocRdMode32() {
        return SPI_IOC_RD_MODE32;
    }

    public static long getSpiIocWrMode32() {
        return SPI_IOC_WR_MODE32;
    }

    public static long getSpiIocRdLsbFirst() {
        return SPI_IOC_RD_LSB_FIRST;
    }

    public static long getSpiIocWrLsbFirst() {
        return SPI_IOC_WR_LSB_FIRST;
    }

    public static long getGpioGetLineHandleIoctl() {
        return GPIO_GET_LINEHANDLE_IOCTL;
    }

    public static long getGpioGetLineeventIoctl() {
        return GPIO_GET_LINEEVENT_IOCTL;
    }

    public static long getGpioHandleGetLineValuesIoctl() {
        return GPIOHANDLE_GET_LINE_VALUES_IOCTL;
    }

    public static long getGpioHandleSetLineValuesIoctl() {
        return GPIOHANDLE_SET_LINE_VALUES_IOCTL;
    }

    public static long GET_SPI_IOC_MAGIC() {
        return SPI_IOC_MAGIC;
    }

    public static long IOC_NRBITS() {
        return _IOC_NRBITS;
    }

    public static long IOC_TYPEBITS() {
        return _IOC_TYPEBITS;
    }

    public static long IOC_SIZEBITS() {
        return _IOC_SIZEBITS;
    }

    public static long IOC_DIRBITS() {
        return _IOC_DIRBITS;
    }

    public static long IOC_NRSHIFT() {
        return _IOC_NRSHIFT;
    }

    public static long IOC_NONE() {
        return _IOC_NONE;
    }

    public static long IOC_READ() {
        return _IOC_READ;
    }

    public static long IOC_WRITE() {
        return _IOC_WRITE;
    }

    public static int SPI_MSGSIZE(int N) {
        return ((((N) * (SPI_IOC_TRANSFER_SIZE())) < (1 << IOC_SIZEBITS()))
                ? ((N) * (SPI_IOC_TRANSFER_SIZE()))
                : 0);
    }

    private static int SPI_IOC_TRANSFER_SIZE() {
        if (_SPI_IOC_TRANSFER_SIZE == -1) {
            _SPI_IOC_TRANSFER_SIZE = 44;
        }
        return _SPI_IOC_TRANSFER_SIZE;
    }

    public static long SPI_IOC_MESSAGE(int N) {
        return _IOW(GET_SPI_IOC_MAGIC(), 0, SPI_MSGSIZE(N));
    }
    

    private long _IOC_NRMASK() {
        return ((1L << IOC_NRBITS()) - 1);
    }

    private long _IOC_TYPEMASK() {
        return ((1L << IOC_TYPEBITS()) - 1);
    }

    private long _IOC_SIZEMASK() {
        return ((1L << IOC_SIZEBITS()) - 1);
    }

    private long _IOC_DIRMASK() {
        return ((1L << IOC_DIRBITS()) - 1);
    }

    private static long _IOC_TYPESHIFT() {
        return IOC_NRSHIFT() + IOC_NRBITS();
    }

    private static long _IOC_SIZESHIFT() {
        return _IOC_TYPESHIFT() + IOC_TYPEBITS();
    }

    private static long _IOC_DIRSHIFT() {
        return _IOC_SIZESHIFT() + IOC_SIZEBITS();
    }

    private long _IO(long type, long nr) {
        return _IOC(IOC_NONE(), (type), (nr), 0);
    }

    private static long _IOR(long type, long nr, long size) {
        return _IOC(IOC_READ(), type, nr, size);
    }

    private static long _IOW(long type, long nr, long size) {
        return _IOC(IOC_WRITE(), type, nr, size);
    }

    private static long _IOWR(long type, long nr, long size) {
        return _IOC(IOC_READ() | IOC_WRITE(), type, nr, size);
    }

    private static long _IOC(long dir, long type, long nr, long size) {
        return (((dir) << _IOC_DIRSHIFT()) |
                ((type) << _IOC_TYPESHIFT()) |
                ((nr) << IOC_NRSHIFT()) |
                ((size) << _IOC_SIZESHIFT()));
    }

    static {
        SPI_IOC_RD_MODE = _IOR(GET_SPI_IOC_MAGIC(), 1, 1);
        SPI_IOC_WR_MODE = _IOW(GET_SPI_IOC_MAGIC(), 1, 1);
        SPI_IOC_RD_BITS_PER_WORD = _IOR(GET_SPI_IOC_MAGIC(), 3, 1);
        SPI_IOC_WR_BITS_PER_WORD = _IOW(GET_SPI_IOC_MAGIC(), 3, 1);
        SPI_IOC_RD_MAX_SPEED_HZ = _IOR(GET_SPI_IOC_MAGIC(), 4, 4);
        SPI_IOC_WR_MAX_SPEED_HZ = _IOW(GET_SPI_IOC_MAGIC(), 4, 4);
        SPI_IOC_RD_MODE32 = _IOR(GET_SPI_IOC_MAGIC(), 5, 4);
        SPI_IOC_WR_MODE32 = _IOW(GET_SPI_IOC_MAGIC(), 5, 4);
        SPI_IOC_RD_LSB_FIRST = _IOR(GET_SPI_IOC_MAGIC(), 2, 1);
        SPI_IOC_WR_LSB_FIRST = _IOW(GET_SPI_IOC_MAGIC(), 2, 1);

        GPIO_GET_CHIPINFO_IOCTL = _IOR(0xB4, 0x01, 68);
        GPIO_GET_LINEINFO_IOCTL = _IOWR(0xB4, 0x02, 72);
        GPIO_GET_LINEHANDLE_IOCTL = _IOWR(0xB4, 0x03, 364);
        GPIO_GET_LINEEVENT_IOCTL = _IOWR(0xB4, 0x04, 48);

        GPIOHANDLE_GET_LINE_VALUES_IOCTL = _IOWR(0xB4, 0x08, 64);
        GPIOHANDLE_SET_LINE_VALUES_IOCTL = _IOWR(0xB4, 0x09, 64);
    }
}
