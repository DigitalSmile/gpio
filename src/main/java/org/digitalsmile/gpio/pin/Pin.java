package org.digitalsmile.gpio.pin;

import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.digitalsmile.gpio.pin.attributes.*;
import org.digitalsmile.gpio.pin.structs.HandleDataStruct;
import org.digitalsmile.gpio.pin.structs.HandleRequestStruct;
import org.digitalsmile.gpio.pin.structs.LineInfoStruct;
import org.digitalsmile.gpio.core.file.Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

public final class Pin {
    private static final Logger logger = LoggerFactory.getLogger(Pin.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final String deviceName;
    private final int pin;
    private final LineInfoStruct lineInfoStruct;
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService eventTaskProcessor = Executors.newVirtualThreadPerTaskExecutor();

    private int fd;
    private State state;
    private Direction direction;
    private boolean closed = false;

    public Pin(String deviceName, int gpioPin, Direction direction) throws IOException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new IOException("Wrong call of constructor, Pin should be created by using GPIOBoard.ofPin(...) methods.");
        }
        this.deviceName = deviceName;
        this.pin = gpioPin;
        logger.info("{}-{} - setting up GPIO Pin...", deviceName, gpioPin);
        logger.debug("{}-{} - opening device file.", deviceName, gpioPin);
        this.fd = FileDescriptor.open(deviceName, Flags.O_RDONLY | Flags.O_CLOEXEC);
        var data = new LineInfoStruct(gpioPin, 0, new byte[]{}, new byte[]{});
        logger.debug("{}-{} - getting line info.", deviceName, gpioPin);
        this.lineInfoStruct = IOCtl.call(fd, Command.getGpioGetLineInfoIoctl(), data);
        if ((lineInfoStruct.flags() & Flag.KERNEL.getValue()) > 0) {
            close();
            throw new IOException("Pin " + pin + " is blocked by Kernel");
        }
        setDirection(direction);
    }

    public String getName() {
        return new String(lineInfoStruct.name());
    }

    public int getPinNumber() {
        return pin;
    }

    public State getState() {
        return state;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) throws IOException {
        checkClosed();
        if (direction.equals(this.direction)) {
            logger.warn("{}-{} - direction {} is already set.", deviceName, pin, direction);
            return;
        }
        logger.debug("{}-{} - setting direction to {}.", deviceName, pin, direction);
        var label = "ffm-io".getBytes();
        var gpioHandleRequest = new HandleRequestStruct(new int[]{pin}, direction.getMode(), new byte[]{}, label, 1, 0);
        var result = IOCtl.call(fd, Command.getGpioGetLineHandleIoctl(), gpioHandleRequest);
        this.fd = result.fd();
        this.direction = direction;
    }

    public void close() {
        logger.info("{}-{} - closing GPIO Pin.", deviceName, pin);
        FileDescriptor.close(fd);
        executorService.close();
        this.watcher = null;
        this.closed = true;
        logger.info("{}-{} - GPIO Pin is closed. Recreate the pin object to reuse.", deviceName, pin);
    }

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

    public void write(State state) throws IOException {
        checkClosed();
        checkDirection();
        if (Direction.INPUT.equals(this.direction)) {
            throw new IOException("Can't write to input pin " + new String(lineInfoStruct.name()) + ". The direction is set to input.");
        }
        logger.trace("{}-{} - setting GPIO Pin to state {}.", deviceName, pin, state);
        var gpioHandleData = new HandleDataStruct(new byte[]{state.getState()});
        IOCtl.call(fd, Command.getGpioHandleSetLineValuesIoctl(), gpioHandleData);
        this.state = state;
    }


    private void checkDirection() throws IOException {
        if (direction == null) {
            throw new IOException("Pin " + pin + " direction not set");
        }
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Pin " + pin + " is closed");
        }
    }

    private Runnable watcher;

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

    private class EventWatcher implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(EventWatcher.class);

        private final Event event;
        private final PinEventProcessing eventProcessor;

        private State currentState;

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
