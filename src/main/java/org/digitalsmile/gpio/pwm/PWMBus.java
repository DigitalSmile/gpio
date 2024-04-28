package org.digitalsmile.gpio.pwm;

import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.NativeMemoryException;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.file.FileFlag;
import org.digitalsmile.gpio.pwm.attributes.PWMPolarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Class for creating PWMBus object through sysfs. It uses native FFM calls (such as open/close/read/write) to operate with hardware.
 * Instance of PWMBus can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
 */
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
    private long dutyCycle;
    private long period;
    private PWMPolarity pwmPolarity;

    private int frequency;
    private int speed;

    /**
     * Creates PWMus instance with given PWM chip number and bus number.
     *
     * @param pwmChipNumber PWM chip number, located at sysfs
     * @param pwmBusNumber  PWM Bus number, located at sysfs
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public PWMBus(int pwmChipNumber, int pwmBusNumber) throws NativeMemoryException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new RuntimeException("Wrong call of constructor, PWMBus should be created by using GPIOBoard.ofPWMBus(...) methods.");
        }

        var pwmChipFile = Path.of(CHIP_PATH + pwmChipNumber).toFile();
        if (!pwmChipFile.exists()) {
            throw new IllegalArgumentException("PWM Chip at path '" + pwmChipFile.getPath() + "' does not exist!");
        }
        var pwmFile = Path.of(pwmChipFile.getPath() + PWM_PATH + pwmBusNumber).toFile();
        if (!pwmFile.exists()) {
            logger.warn("{} - no PWM Bus found... will try to export PWM Bus first.", pwmFile);
            var npwmFd = FileDescriptor.open(pwmChipFile.getPath() + CHIP_NPWM_PATH, FileFlag.O_RDONLY);
            var maxChannel = getIntegerContent(FileDescriptor.read(npwmFd, MAX_FILE_SIZE));
            FileDescriptor.close(npwmFd);
            if (pwmBusNumber > maxChannel - 1) {
                throw new IllegalArgumentException("PWM Bus at path '" + pwmFile.getPath() + "' cannot be exported! Max available channel is " + maxChannel);
            }
            var exportFd = FileDescriptor.open(pwmChipFile.getPath() + CHIP_EXPORT_PATH, FileFlag.O_WRONLY);
            FileDescriptor.write(exportFd, String.valueOf(pwmBusNumber));
            FileDescriptor.close(exportFd);
            if (!pwmFile.exists()) {
                throw new IllegalArgumentException("PWM Bus at path '" + pwmFile.getPath() + "' cannot be exported!");
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
        this.pwmPolarity = PWMPolarity.getPolarityByString(new String(FileDescriptor.read(polarityFd, MAX_FILE_SIZE)).trim());

        FileDescriptor.close(enableFd);
        FileDescriptor.close(dutyCycleFd);
        FileDescriptor.close(periodFd);
        FileDescriptor.close(polarityFd);

        logger.info("{} - pwm setup finished. Initial state: {}", pwmPath, this);
    }

    /**
     * Configures PWM Bus with given frequency and speed.
     *
     * @param frequency frequency in hertz
     * @param speed     rotation speed from 0 to 100%
     * @throws NativeMemoryException if any error occurred during configuration
     */
    public void configure(int frequency, int speed) throws NativeMemoryException {
        logger.info("{} - setting frequency to {}Hz, speed to {}%.", pwmPath, frequency, speed);
        configureInternal(frequency, speed);
    }

    /**
     * Configures PWM Bus with given frequency, speed and polarity.
     *
     * @param frequency   frequency in hertz
     * @param speed       rotation speed from 0 to 100%
     * @param pwmPolarity polarity of PWM (normal or inversed)
     * @throws NativeMemoryException if any error occurred during configuration
     */
    public void configure(int frequency, int speed, PWMPolarity pwmPolarity) throws NativeMemoryException {
        logger.info("{} - setting frequency to {}Hz, speed to {}% and polarity to {}.", pwmPath, frequency, speed, pwmPolarity);
        configureInternal(frequency, speed);
        setPolarity(pwmPolarity);
    }

    /**
     * Internal method for configuring PWM Bus.
     *
     * @param frequency frequency in hertz
     * @param speed     rotation speed from 0 to 100%
     * @throws NativeMemoryException if any error occurred during configuration
     */
    private void configureInternal(int frequency, int speed) throws NativeMemoryException {
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

        this.period = period;
        this.dutyCycle = dutyCycle;
    }

    /**
     * Enables PWM Bus and start wave generation.
     *
     * @throws NativeMemoryException if any error occurred
     */
    public void enable() throws NativeMemoryException {
        if (enabled) {
            logger.warn("{} - PWM Bus is already enabled.", pwmPath);
            return;
        }
        checkPeriodNotZero();
        var enableFd = FileDescriptor.open(this.pwmPath + ENABLE_PATH);
        FileDescriptor.write(enableFd, String.valueOf(1).getBytes());
        FileDescriptor.close(enableFd);
        this.enabled = true;
    }

    /**
     * Disables PWM Bus and stops wave generation.
     *
     * @throws NativeMemoryException if any error occurred
     */
    public void disable() throws NativeMemoryException {
        if (!enabled) {
            logger.warn("{} - PWM Bus is already disabled.", pwmPath);
            return;
        }
        var enableFd = FileDescriptor.open(this.pwmPath + ENABLE_PATH);
        FileDescriptor.write(enableFd, String.valueOf(0).getBytes());
        FileDescriptor.close(enableFd);
        this.enabled = false;
    }

    /**
     * Sets the polarity of PWM Bus.
     *
     * @param pwmPolarity polarity of PWM Bus
     * @throws NativeMemoryException if any error occurred
     */
    public void setPolarity(PWMPolarity pwmPolarity) throws NativeMemoryException {
        if (this.pwmPolarity.equals(pwmPolarity)) {
            logger.warn("{} - polarity is already set to {}.", pwmPath, pwmPolarity);
            return;
        }
        if (this.enabled) {
            logger.warn("{} - changing polarity while PWM is enabled will cause no effect. " +
                    "You should run disable() and then enable() to take effect!", pwmPath);
        }
        checkPeriodNotZero();
        logger.info("{} - changing polarity to {}", pwmPath, pwmPolarity);
        var polarityFd = FileDescriptor.open(this.pwmPath + POLARITY_PATH, FileFlag.O_WRONLY);
        FileDescriptor.write(polarityFd, pwmPolarity.getPolarity().getBytes());
        FileDescriptor.close(polarityFd);
        this.pwmPolarity = pwmPolarity;
    }

    /**
     * Sets rotation speed of PWM Bus.
     *
     * @param speed rotation speed from 0 to 100%
     * @throws NativeMemoryException if any error occurred
     */
    public void setSpeed(int speed) throws NativeMemoryException {
        checkPeriodNotZero();
        logger.info("{} - setting speed to {}%.", pwmPath, speed);
        configureInternal(frequency, speed);
    }

    /**
     * Sets frequency of PWM Bus.
     *
     * @param frequency frequency in hertz
     * @throws NativeMemoryException if any error occurred
     */
    public void setFrequency(int frequency) throws NativeMemoryException {
        logger.info("{} - setting frequency to {}Hz.", pwmPath, frequency);
        configureInternal(frequency, speed);
    }

    /**
     * Gets PWM Bus polarity.
     *
     * @return PWM Bus polarity
     */
    public PWMPolarity getPolarity() {
        return pwmPolarity;
    }

    /**
     * Checks if PWM Bus is enabled.
     *
     * @return true if PWM Bus is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets rotation speed of PWM Bus.
     *
     * @return rotation speed
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Gets frequency of PWM Bus.
     *
     * @return frequency of PWM Bus
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * Checks if frequency is zero.
     *
     * @throws NativeMemoryException if frequency is zero
     */
    private void checkPeriodNotZero() throws NativeMemoryException {
        if (this.frequency == 0) {
            logger.error("{} - frequency is 0, please specify the working PWM frequency first.", pwmPath);
            throw new NativeMemoryException("Frequency is not specified!");
        }
    }

    /**
     * Since read/write of file descriptors accepts only byte arrays / text, we have to convert inputs from text bytes to numbers.
     *
     * @param bytes text byte array to be converted
     * @return integer representation of text byte array
     */
    private static int getIntegerContent(byte[] bytes) {
        return Integer.parseInt(new String(bytes).trim());
    }

    @Override
    public String toString() {
        return "PWMPin{" +
                " enabled=" + enabled +
                ", dutyCycle=" + dutyCycle +
                ", period=" + period +
                ", polarity='" + pwmPolarity + '\'' +
                '}';
    }
}
