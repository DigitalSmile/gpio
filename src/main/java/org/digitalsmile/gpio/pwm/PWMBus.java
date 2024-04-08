package org.digitalsmile.gpio.pwm;

import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.core.exception.NativeException;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.file.FileFlag;
import org.digitalsmile.gpio.pwm.attributes.Polarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class PWMBus {
    private static final Logger logger = LoggerFactory.getLogger(PWMBus.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final String CHIP_PATH = "/sys/class/pwm/pwmchip";
    private static final String CHIP_EXPORT_PATH = "/export";
    private static final String CHIP_NPWM_PATH = "/npwm";
    private static final String PWM_PATH = "/pwm";
    private static final String ENABLE_PATH = "/enable";
    private static final String DUTY_CYCLE_PATH = "/duty_cycle";
    private static final String PERIOD_PATH = "/period";
    private static final String POLARITY_PATH = "/polarity";

    /**
     * Since the real size of files in sysfs cannot be determined until read,
     * we assume the max value for the files in PWM is int32 (2 ^ 32).
     * Files are text ASCII (not binary), so the max int in string representation is 10 bytes.
     */
    private static final int MAX_FILE_SIZE = 10;
    private static final long NANOS_IN_SECOND = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

    private final String pwmPath;

    private boolean enabled;
    private int dutyCycle;
    private int period;
    private Polarity polarity;

    private int frequency;
    private int speed;

    public PWMBus(int pwmChipNumber, int pwmBusNumber) throws NativeException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new RuntimeException("Wrong call of constructor, PWMBus should be created by using GPIOBoard.ofPWMBus(...) methods.");
        }

        var pwmChipFile = Path.of(CHIP_PATH + pwmChipNumber).toFile();
        if (!pwmChipFile.exists()) {
            throw new IllegalArgumentException("Chip at path '" + pwmChipFile.getPath() + "' does not exist!");
        }
        var pwmFile = Path.of(pwmChipFile.getPath() + PWM_PATH + pwmBusNumber).toFile();
        if (!pwmFile.exists()) {
            logger.warn("{} - no pwm found... will try to export chip first.", pwmFile);
            var npwmFd = FileDescriptor.open(pwmChipFile.getPath() + CHIP_NPWM_PATH, FileFlag.O_RDONLY);
            var channels = getIntegerContent(FileDescriptor.read(npwmFd, MAX_FILE_SIZE));
            FileDescriptor.close(npwmFd);
            if (pwmBusNumber > channels - 1) {
                throw new IllegalArgumentException("PWM at path '" + pwmFile.getPath() + "' cannot be exported!");
            }
            var exportFd = FileDescriptor.open(pwmChipFile.getPath() + CHIP_EXPORT_PATH, FileFlag.O_WRONLY);
            FileDescriptor.write(exportFd, String.valueOf(pwmBusNumber).getBytes());
            FileDescriptor.close(exportFd);
            if (!pwmFile.exists()) {
                throw new IllegalArgumentException("PWM at path '" + pwmFile.getPath() + "' cannot be exported!");
            }
        }
        this.pwmPath = pwmFile.getPath();

        var enableFd = FileDescriptor.open(this.pwmPath + ENABLE_PATH);
        var dutyCycleFd = FileDescriptor.open(this.pwmPath + DUTY_CYCLE_PATH);
        var periodFd = FileDescriptor.open(this.pwmPath + PERIOD_PATH);
        var polarityFd = FileDescriptor.open(this.pwmPath + POLARITY_PATH);

        this.enabled = getIntegerContent(FileDescriptor.read(enableFd, MAX_FILE_SIZE)) == 1;
        this.dutyCycle = getIntegerContent(FileDescriptor.read(dutyCycleFd, MAX_FILE_SIZE));
        this.period = getIntegerContent(FileDescriptor.read(periodFd, MAX_FILE_SIZE));
        this.polarity = Polarity.getPolarityByString(new String(FileDescriptor.read(polarityFd, MAX_FILE_SIZE)).trim());

        FileDescriptor.close(enableFd);
        FileDescriptor.close(dutyCycleFd);
        FileDescriptor.close(periodFd);
        FileDescriptor.close(polarityFd);

        logger.info("{} - pwm setup finished. Initial state: {}", this.pwmPath, this);
    }

    public void configure(int frequency, int speed) throws NativeException {
        logger.info("{} - setting frequency to {}Hz, speed to {}%.", pwmPath, frequency, speed);
        configureInternal(frequency, speed);
    }

    public void configure(int frequency, int speed, Polarity polarity) throws NativeException {
        logger.info("{} - setting frequency to {}Hz, speed to {}% and polarity to {}.", pwmPath, frequency, speed, polarity);
        configureInternal(frequency, speed);
        setPolarity(polarity);
    }

    private void configureInternal(int frequency, int speed) throws NativeException {
        if (frequency < 0) {
            logger.error("{} - cannot set frequency '{}', required more then 0.", pwmPath, frequency);
            return;
        }
        if (speed < 0 || speed > 100) {
            logger.error("{} - cannot set speed '{}', required more 0% and less 100%.", pwmPath, speed);
            return;
        }

        this.speed = speed;
        this.frequency = frequency;
        logger.debug("{} - frequency is {}Hz and speed {}%.", pwmPath, frequency, speed);

        var period = (NANOS_IN_SECOND / frequency);
        var dutyCycle = (int) ((((NANOS_IN_SECOND / frequency) * speed) / 100f));
        logger.debug("{} - period is '{}' and dutyCycle is '{}'.", pwmPath, period, dutyCycle);

        var periodFd = FileDescriptor.open(this.pwmPath + PERIOD_PATH, FileFlag.O_WRONLY);
        var dutyCycleFd = FileDescriptor.open(this.pwmPath + DUTY_CYCLE_PATH, FileFlag.O_WRONLY);

        FileDescriptor.write(periodFd, String.valueOf(period).getBytes());
        FileDescriptor.write(dutyCycleFd, String.valueOf(dutyCycle).getBytes());

        FileDescriptor.close(dutyCycleFd);
        FileDescriptor.close(periodFd);
    }

    public void enable() throws NativeException {
        if (enabled) {
            logger.warn("{} - PWM is already enabled.", pwmPath);
            return;
        }
        checkPeriodNotZero();
        var enableFd = FileDescriptor.open(this.pwmPath + ENABLE_PATH);
        FileDescriptor.write(enableFd, String.valueOf(1).getBytes());
        FileDescriptor.close(enableFd);
        this.enabled = true;
    }

    public void disable() throws NativeException {
        if (!enabled) {
            logger.warn("{} - PWM is already disabled.", pwmPath);
            return;
        }
        var enableFd = FileDescriptor.open(this.pwmPath + ENABLE_PATH);
        FileDescriptor.write(enableFd, String.valueOf(0).getBytes());
        FileDescriptor.close(enableFd);
        this.enabled = false;
    }


    public void setPolarity(Polarity polarity) throws NativeException {
        if (this.polarity.equals(polarity)) {
            logger.warn("{} - polarity is already set to {}.", pwmPath, polarity);
            return;
        }
        if (this.enabled) {
            logger.warn("{} - changing polarity while PWM is enabled will cause no effect. " +
                    "You should run disable() and then enable() to take effect!", pwmPath);
        }
        checkPeriodNotZero();
        logger.info("{} - changing polarity to {}", pwmPath, polarity);
        var polarityFd = FileDescriptor.open(this.pwmPath + POLARITY_PATH, FileFlag.O_WRONLY);
        FileDescriptor.write(polarityFd, polarity.getPolarity().getBytes());
        FileDescriptor.close(polarityFd);
        this.polarity = polarity;
    }

    public void setSpeed(int speed) throws NativeException {
        checkPeriodNotZero();
        logger.info("{} - setting speed to {}%.", pwmPath, speed);
        configureInternal(frequency, speed);
    }

    public void setFrequency(int frequency) throws NativeException {
        logger.info("{} - setting frequency to {}Hz.", pwmPath, frequency);
        configureInternal(frequency, speed);
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSpeed() {
        return speed;
    }

    public int getFrequency() {
        return frequency;
    }

    private void checkPeriodNotZero() throws NativeException {
        if (this.frequency == 0) {
            logger.error("{} - frequency is 0, please specify the working PWM frequency first.", pwmPath);
            throw new NativeException("Frequency is not specified!");
        }
    }

    private static int getIntegerContent(byte[] bytes) {
        return Integer.parseInt(new String(bytes).trim());
    }

    @Override
    public String toString() {
        return "PWMPin{" +
                " enabled=" + enabled +
                ", dutyCycle=" + dutyCycle +
                ", period=" + period +
                ", polarity='" + polarity + '\'' +
                '}';
    }
}
