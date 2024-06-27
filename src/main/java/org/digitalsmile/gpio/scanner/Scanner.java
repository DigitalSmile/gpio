package org.digitalsmile.gpio.scanner;

import io.github.digitalsmile.annotation.function.NativeMemoryException;
import org.digitalsmile.gpio.*;
import org.digitalsmile.gpio.core.IntegerToHex;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.file.FileDescriptorNative;
import org.digitalsmile.gpio.core.file.FileFlag;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.Ioctl;
import org.digitalsmile.gpio.core.ioctl.IoctlNative;
import org.digitalsmile.gpio.i2c.attributes.I2CStatus;
import org.digitalsmile.gpio.pin.attributes.PinFlag;
import org.digitalsmile.gpio.pin.structs.ChipInfo;
import org.digitalsmile.gpio.pin.structs.LineAttribute;
import org.digitalsmile.gpio.pin.structs.LineInfo;
import org.digitalsmile.gpio.scanner.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Scanner {
    private static final Logger logger = LoggerFactory.getLogger(Scanner.class);
    private static final Ioctl IOCTL = new IoctlNative();
    private static final FileDescriptor FILE = new FileDescriptorNative();

    private static final List<GPIODevice> gpioDevices = new ArrayList<>();
    private static final List<I2CDevice> i2cDevices = new ArrayList<>();
    private static final List<PWBDevice> pwmDevices = new ArrayList<>();
    private static final List<SPIDevice> spiDevices = new ArrayList<>();
    private static String socModel;
    private static String osVersion;

    private static final String GPIO_PATH = "/dev/gpiochip";
    private static final String I2C_PATH = "/dev/i2c-";
    private static final String PWM_CHIP_PATH = "/sys/class/pwm/pwmchip";
    private static final String PWM_NPWM_PATH = "/npwm";
    private static final String SOC_MODEL = "/sys/firmware/devicetree/base/model";
    private static final String OS = "/etc/os-release";
    private static final String SPI_PATH = "/dev/spidev0.";

    private Scanner(){}

    public static void scan() throws IOException, NativeMemoryException {
        logger.debug("Scanning GPIO Board for available interfaces... This might take a while");
        var socPath = Path.of(SOC_MODEL);
        if (socPath.toFile().exists()) {
            socModel = Files.readString(socPath);
        }
        var osPath = Path.of(OS);
        if (osPath.toFile().exists()) {
            var osInfo = Files.readAllLines(osPath);
            osVersion = osInfo.stream().filter(line -> line.contains("PRETTY_NAME")).findFirst().orElseThrow().split("=")[1].replace("\"", "");
        }
        for (int i = 0; i < 255; i++) {
            var gpiochipFile = Path.of(GPIO_PATH + i).toFile();
            if (gpiochipFile.exists()) {
                var gpioFd = FILE.open(gpiochipFile.getPath(), FileFlag.O_RDWR);
                var chipInfo = IOCTL.call(gpioFd, Command.getGpioGetChipInfoIoctl(), ChipInfo.createEmpty());
                List<GPIOLine> lines = new ArrayList<>();
                for (int pin = 0; pin < chipInfo.lines(); pin++) {
                    var lineInfo = new LineInfo(new byte[]{}, new byte[]{}, pin, 0, 0, new LineAttribute[]{}, new int[]{});
                    lineInfo = IOCTL.call(gpioFd, Command.getGpioV2GetLineInfoIoctl(), lineInfo);
                    List<PinFlag> pinFlags = new ArrayList<>();
                    for (PinFlag flag : PinFlag.values()) {
                        if ((lineInfo.flags() & flag.getValue()) != 0) {
                            pinFlags.add(flag);
                        }
                    }
                    lines.add(new GPIOLine(
                            pin,
                            new String(lineInfo.name()).trim(),
                            new String(lineInfo.consumer()).trim(),
                            pinFlags
                    ));
                }
                logger.debug("Found GPIO Device '{}' with {} pins.", gpiochipFile.getPath(), lines.size());
                gpioDevices.add(new GPIODevice(gpiochipFile.getPath(), new String(chipInfo.name()).trim(), new String(chipInfo.label()).trim(), lines));
                FILE.close(gpioFd);
            }
        }
        for (int i = 0; i < 255; i++) {
            var i2cFile = Path.of(I2C_PATH + i).toFile();
            if (i2cFile.exists()) {
                logger.debug("Found I2C Bus '{}'. Will try to initialize and scan for available devices.", i2cFile);
                try (var i2c = GPIOBoard.ofI2C(I2C_PATH, i)) {
                    var scan = i2c.scan();
                    List<I2CAddress> addresses = new ArrayList<>();
                    for (Map.Entry<Integer, I2CStatus> entry : scan.entrySet()) {
                        if (entry.getValue().equals(I2CStatus.NOT_AVAILABLE)) {
                            continue;
                        }
                        addresses.add(new I2CAddress(entry.getKey(), entry.getValue()));
                    }
                    i2cDevices.add(new I2CDevice(i2cFile.getPath(), i2c.getFunctionalities(), addresses));
                }
            }
        }
        for (int i = 0; i < 255; i++) {
            var pwmChipPath = Path.of(PWM_CHIP_PATH + i).toFile();
            if (pwmChipPath.exists()) {
                var buses = Integer.parseInt(Files.readString(Path.of(pwmChipPath.getPath() + PWM_NPWM_PATH)).trim());
                for (int j = 0; j < buses; j++) {
                    logger.debug("Found PWM Bus '{}'", pwmChipPath.getPath() + "/pwm" + j);
                    pwmDevices.add(new PWBDevice(pwmChipPath.getPath() + "/pwm" + j));
                }
            }

        }
        for (int i = 0; i < 255; i++) {
            var spi = Path.of(SPI_PATH + i).toFile();
            if (spi.exists()) {
                spiDevices.add(new SPIDevice(spi.getPath()));
                logger.debug("Found SPI Bus '{}'", spi.getPath());
            }
        }
        logger.debug("Done scanning.");
    }

    private static String prettyPrintAll() {
        for (GPIODevice gpioDevice : gpioDevices) {
            System.out.println("GPIO chip path: " + gpioDevice.path() + " | Name (label): " + gpioDevice.name() + " (" + gpioDevice.label() + ")");
            var tl = new TableList(4, "PIN", "Name", "Consumer", "Flags").withUnicode(true);
            for (GPIOLine gpioLine : gpioDevice.lines()) {
                tl.addRow(String.valueOf(gpioLine.pin()),
                        gpioLine.name().isEmpty() ? "-" : gpioLine.name(),
                        gpioLine.consumer().isEmpty() ? "-" : gpioLine.consumer(),
                        gpioLine.flags().toString());
            }
            tl.print();
            System.out.println();
        }
        for (I2CDevice i2cDevice : i2cDevices) {
            System.out.println("I2C chip path: " + i2cDevice.path() + " | Functions: " + i2cDevice.functionalities().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList());
            if (i2cDevice.addresses().isEmpty()) {
                System.out.println("No available addresses found on the chip.");
            } else {
                var tl = new TableList(2, "Address", "Status").withUnicode(true);
                for (I2CAddress address : i2cDevice.addresses()) {
                    tl.addRow(String.valueOf(IntegerToHex.convert(address.address())), address.status().toString());
                }
                tl.print();
            }
            System.out.println();
        }
        return "";
    }

}
