package org.digitalsmile.gpio.pin;

import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.digitalsmile.gpio.pin.attributes.*;
import org.digitalsmile.gpio.pin.event.Event;
import org.digitalsmile.gpio.pin.event.PinEventProcessing;
import org.digitalsmile.gpio.pin.structs.HandleDataStruct;
import org.digitalsmile.gpio.pin.structs.HandleRequestStruct;
import org.digitalsmile.gpio.pin.structs.LineInfoStruct;
import org.digitalsmile.gpio.core.file.Flag;
import org.digitalsmile.gpio.spi.SPIBus;
import org.digitalsmile.gpio.spi.SPIMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Class for creating GPIO Pin object. It uses native FFM calls (such as open and ioctl) to operate with hardware.
 * Instance of Pin can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
 */
public final class Pin {
    private static final Logger logger = LoggerFactory.getLogger(Pin.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final String deviceName;
    private final int pin;
    private final LineInfoStruct lineInfoStruct;
    // executor services for event watcher
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService eventTaskProcessor = Executors.newVirtualThreadPerTaskExecutor();
    private Runnable watcher;

    private int fd;
    private State state;
    private Direction direction;
    private boolean closed = false;

    /**
     * Constructs GPIO Pin class from gpio device name, pin and direction (INPUT / OUTPUT).
     * Instance of Pin can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
     *
     * @param deviceName - gpio device name
     * @param gpioPin    - pin gpio number
     * @param direction  - direction, e.g. write or read
     * @throws IOException if errors occurred during creating instance
     */
    public Pin(String deviceName, int gpioPin, Direction direction) throws IOException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new IOException("Wrong call of constructor, Pin should be created by using GPIOBoard.ofPin(...) methods.");
        }
        this.deviceName = deviceName;
        this.pin = gpioPin;
        logger.info("{}-{} - setting up GPIO Pin...", deviceName, gpioPin);
        logger.debug("{}-{} - opening device file.", deviceName, gpioPin);
        this.fd = FileDescriptor.open(deviceName, Flag.O_RDONLY | Flag.O_CLOEXEC);
        var data = new LineInfoStruct(gpioPin, 0, new byte[]{}, new byte[]{});
        logger.debug("{}-{} - getting line info.", deviceName, gpioPin);
        this.lineInfoStruct = IOCtl.call(fd, Command.getGpioGetLineInfoIoctl(), data);
        if ((lineInfoStruct.flags() & org.digitalsmile.gpio.pin.attributes.Flag.KERNEL.getValue()) > 0) {
            close();
            throw new IOException("Pin " + pin + " is blocked by Kernel");
        }
        setDirection(direction);
        logger.info("{}-{} - GPIO Pin configured.", deviceName, gpioPin);
    }

    /**
     * Gets the name of pin from GPIO device.
     *
     * @return the name of pin from GPIO device
     */
    public String getName() {
        return new String(lineInfoStruct.name());
    }

    /**
     * Gets the pin GPIO number.
     *
     * @return the pin GPIO number
     */
    public int getPinNumber() {
        return pin;
    }

    /**
     * Gets the pin state.
     *
     * @return the pin state
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the pin direction (INPUT / OUTPUT)
     *
     * @return pin direction (INPUT / OUTPUT)
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the direction of GPIO Pin.
     *
     * @param direction - new direction for GPIO Pin
     * @throws IOException if errors occurred during direction change
     */
    public void setDirection(Direction direction) throws IOException {
        checkClosed();
        if (direction.equals(this.direction)) {
            logger.warn("{}-{} - direction {} is already set.", deviceName, pin, direction);
            return;
        }
        logger.debug("{}-{} - setting direction to {}.", deviceName, pin, direction);
        var label = "java-gpio".getBytes();
        var gpioHandleRequest = new HandleRequestStruct(new int[]{pin}, direction.getMode(), new byte[]{}, label, 1, 0);
        var result = IOCtl.call(fd, Command.getGpioGetLineHandleIoctl(), gpioHandleRequest);
        this.fd = result.fd();
        this.direction = direction;
    }

    /**
     * Closes the GPIO Pin. Object must be recreated if you have to use it after.
     */
    public void close() {
        logger.info("{}-{} - closing GPIO Pin.", deviceName, pin);
        FileDescriptor.close(fd);
        executorService.close();
        this.watcher = null;
        this.closed = true;
        logger.info("{}-{} - GPIO Pin is closed. Recreate the pin object to reuse.", deviceName, pin);
    }

    /**
     * Reads the state of GPIO Pin.
     *
     * @return the state of GPIO Pin
     * @throws IOException if errors occurred during reading the state
     */
    public State read() throws IOException {
        checkClosed();
        checkDirection();
        if (Direction.OUTPUT.equals(this.direction)) {
            throw new IOException("Can't read from output pin " + new String(lineInfoStruct.name()) + ". The direction is set to output");
        }
        logger.trace("{}-{} - reading GPIO Pin.", deviceName, pin);
        var gpioHandleData = new HandleDataStruct(new byte[1]);
        var result = IOCtl.call(fd, Command.getGpioHandleGetLineValuesIoctl(), gpioHandleData);
        this.state = result.values()[0] == 1 ? State.HIGH : State.LOW;
        logger.trace("{}-{} - new GPIO Pin state is {}.", deviceName, pin, state);
        return state;
    }

    /**
     * Writes the state to GPIO Pin.
     *
     * @param state the state to be written
     * @throws IOException if errors occurred during writing new state
     */
    public void write(State state) throws IOException {
        checkClosed();
        checkDirection();
        if (Direction.INPUT.equals(this.direction)) {
            throw new IOException("Can't write to input pin " + new String(lineInfoStruct.name()) + ". The direction is set to input.");
        }
        logger.trace("{}-{} - setting GPIO Pin to state {}.", deviceName, pin, state);
        var gpioHandleData = new HandleDataStruct(new byte[]{state.getValue()});
        IOCtl.call(fd, Command.getGpioHandleSetLineValuesIoctl(), gpioHandleData);
        this.state = state;
    }

    /**
     * Checks if direction is explicitly set.
     *
     * @throws IOException if direction is no set
     */
    private void checkDirection() throws IOException {
        if (direction == null) {
            throw new IOException("Pin " + pin + " direction not set");
        }
    }

    /**
     * Checks if GPIO Pin is closed.
     *
     * @throws IOException if GPIO Pin is closed
     */
    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Pin " + pin + " is closed");
        }
    }

    /**
     * Adds event detection listener, see {@link Event}.
     *
     * @param event          - the event to detect
     * @param eventProcessor - event processor callback
     * @return future to operate the task
     * @throws IOException if watcher is already set for the GPIO Pin
     */
    public ScheduledFuture<?> addEventDetection(Event event, PinEventProcessing eventProcessor) throws IOException {
        if (watcher != null) {
            throw new IOException("Watcher is already set for " + pin + ". " + watcher);
        }
        logger.info("{}-{} - adding event {} detection.", deviceName, pin, event);
        this.watcher = new EventWatcher(event, eventProcessor);
        return executorService.scheduleAtFixedRate(() -> {
            eventTaskProcessor.execute(watcher);
        }, 0, 10, TimeUnit.NANOSECONDS);
    }

    @Override
    public String toString() {
        return "GPIOPin{" +
                "deviceName='" + deviceName + '\'' +
                ", pin=" + pin +
                ", state=" + state +
                ", direction=" + direction +
                ", closed=" + closed +
                '}';
    }

    /**
     * Internal class for watching the event on GPIO Pin.
     */
    private class EventWatcher implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(EventWatcher.class);

        private final Event event;
        private final PinEventProcessing eventProcessor;

        private State currentState;

        /**
         * Constructs the EventWatcher
         *
         * @param event          - event
         * @param eventProcessor - event processor
         */
        EventWatcher(Event event, PinEventProcessing eventProcessor) {
            this.event = event;
            this.eventProcessor = eventProcessor;
            if (event.equals(Event.FALLING)) {
                currentState = State.HIGH;
            } else if (event.equals(Event.RISING)) {
                currentState = State.LOW;
            } else {
                currentState = State.LOW;
            }
        }

        @Override
        public void run() {
            try {
                var newState = read();
                switch (event) {
                    case RISING -> {
                        if (newState.equals(State.HIGH) && currentState.equals(State.LOW)) {
                            logger.trace("{}-{} - detected event {}: new state is {}.", deviceName, pin, event, newState);
                            eventProcessor.process(newState);
                        }
                    }
                    case FALLING -> {
                        if (newState.equals(State.LOW) && currentState.equals(State.HIGH)) {
                            logger.trace("{}-{} - detected event {}: new state is {}.", deviceName, pin, event, newState);
                            eventProcessor.process(newState);
                        }
                    }
                    case BOTH -> {
                        if (!currentState.equals(newState)) {
                            logger.trace("{}-{} - detected event {}: new state is {}.", deviceName, pin, event, newState);
                            eventProcessor.process(newState);
                        }
                    }
                }
                currentState = newState;
            } catch (IOException e) {
                logger.error("{}-{} - error while watching for event {}.", deviceName, pin, event);
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "EventWatcher{" +
                    "event=" + event +
                    ", eventProcessor=" + eventProcessor +
                    '}';
        }
    }
}
