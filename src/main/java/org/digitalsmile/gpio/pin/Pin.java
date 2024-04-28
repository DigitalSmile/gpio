package org.digitalsmile.gpio.pin;

import org.digitalsmile.gpio.GPIOBoard;
import org.digitalsmile.gpio.NativeMemoryException;
import org.digitalsmile.gpio.core.file.FileDescriptor;
import org.digitalsmile.gpio.core.file.FileFlag;
import org.digitalsmile.gpio.core.ioctl.Command;
import org.digitalsmile.gpio.core.ioctl.IOCtl;
import org.digitalsmile.gpio.core.poll.Poll;
import org.digitalsmile.gpio.core.poll.PollFlag;
import org.digitalsmile.gpio.core.poll.PollingData;
import org.digitalsmile.gpio.pin.attributes.PinDirection;
import org.digitalsmile.gpio.pin.attributes.PinEvent;
import org.digitalsmile.gpio.pin.attributes.PinFlag;
import org.digitalsmile.gpio.pin.attributes.PinState;
import org.digitalsmile.gpio.pin.event.DetectedEvent;
import org.digitalsmile.gpio.pin.event.PinEventProcessing;
import org.digitalsmile.gpio.pin.structs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Class for creating GPIO Pin object. It uses native FFM calls (such as open and ioctl) to operate with hardware.
 * Instance of Pin can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
 */
public final class Pin implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Pin.class);
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final String deviceName;
    private final int pin;
    private final LineInfo lineInfo;
    private final int fd;
    private final PinDirection pinDirection;

    private static final ThreadFactory factory = Thread.ofVirtual().name("pin-event-detection-", 0).factory();
    // executor services for event watcher
    private static final ExecutorService eventTaskProcessor = Executors.newThreadPerTaskExecutor(factory);
    private EventWatcher watcher;


    private PinState pinState;
    private boolean closed = false;

    /**
     * Constructs GPIO Pin class from gpio device name, pin and direction (INPUT / OUTPUT).
     * Instance of Pin can only be created from {@link GPIOBoard} class, because we need to initialize GPIO device first and run some validations beforehand.
     *
     * @param deviceName   gpio device name
     * @param gpioPin      pin gpio number
     * @param pinDirection direction, e.g. write or read
     * @throws NativeMemoryException if errors occurred during creating instance
     */
    public Pin(String deviceName, int gpioPin, PinDirection pinDirection) throws NativeMemoryException {
        if (!walker.getCallerClass().equals(GPIOBoard.class)) {
            throw new RuntimeException("Wrong call of constructor, Pin should be created by using GPIOBoard.ofPin(...) methods.");
        }
        this.deviceName = deviceName;
        this.pin = gpioPin;
        logger.info("{}-{} - setting up GPIO Pin...", deviceName, gpioPin);
        logger.debug("{}-{} - opening device file.", deviceName, gpioPin);
        var fd = FileDescriptor.open(deviceName, FileFlag.O_RDONLY | FileFlag.O_CLOEXEC);
        var lineInfo = LineInfo.create(gpioPin);
        logger.debug("{}-{} - getting line info.", deviceName, gpioPin);
        this.lineInfo = IOCtl.call(fd, Command.getGpioV2GetLineInfoIoctl(), lineInfo);
        if ((lineInfo.flags() & PinFlag.USED.getValue()) > 0) {
            close();
            throw new RuntimeException("Pin " + pin + " is in use");
        }
        logger.info("{}-{} - GPIO Pin line info: {}", deviceName, gpioPin, lineInfo);
        // if the direction is input we automatically add event detection to the pin for future use
        var flags = pinDirection.equals(PinDirection.INPUT) ? (PinFlag.EDGE_FALLING.getValue() | PinFlag.EDGE_RISING.getValue()) : 0;
        var lineConfig = new LineConfig(pinDirection.getMode() | flags, 0);
        var lineRequest = LineRequest.create(new int[]{pin}, "org.digitalsmile.gpio", lineConfig);
        var result = IOCtl.call(fd, Command.getGpioV2GetLineIoctl(), lineRequest);
        this.fd = result.fd();

        this.pinDirection = pinDirection;
        logger.info("{}-{} - GPIO Pin configured: {}", deviceName, gpioPin, result);
    }

    /**
     * Gets the name of pin from GPIO device.
     *
     * @return the name of pin from GPIO device
     */
    public String getName() {
        return new String(lineInfo.name());
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
    public PinState getState() {
        return pinState;
    }

    /**
     * Gets the pin direction (INPUT / OUTPUT)
     *
     * @return pin direction (INPUT / OUTPUT)
     */
    public PinDirection getDirection() {
        return pinDirection;
    }

    /**
     * Closes the GPIO Pin. Object must be recreated if you have to use it after.
     *
     * @throws NativeMemoryException if errors occurred during closing file descriptor
     */
    @Override
    public void close() throws NativeMemoryException {
        logger.info("{}-{} - closing GPIO Pin.", deviceName, pin);
        FileDescriptor.close(fd);
        this.watcher = null;
        this.closed = true;
        logger.debug("{}-{} - GPIO Pin is closed. Recreate the pin object to reuse.", deviceName, pin);
    }

    /**
     * Reads the state of GPIO Pin.
     *
     * @return the state of GPIO Pin
     * @throws NativeMemoryException if errors occurred during reading the state
     */
    public PinState read() throws NativeMemoryException {
        checkClosed();
        logger.trace("{}-{} - reading GPIO Pin.", deviceName, pin);
        var lineValues = new LineValues(0, 1);
        var result = IOCtl.call(fd, Command.getGpioV2GetValuesIoctl(), lineValues);
        this.pinState = result.bits() == 1 ? PinState.HIGH : PinState.LOW;
        logger.trace("{}-{} - new GPIO Pin state is {}.", deviceName, pin, pinState);
        return pinState;
    }

    /**
     * Writes the state to GPIO Pin.
     *
     * @param pinState the state to be written
     * @throws NativeMemoryException if errors occurred during writing new state
     */
    public void write(PinState pinState) throws NativeMemoryException {
        checkClosed();
        checkDirection();
        if (PinDirection.INPUT.equals(this.pinDirection)) {
            throw new RuntimeException("Can't write to input pin " + new String(lineInfo.name()) + ". The direction is set to input.");
        }
        logger.trace("{}-{} - setting GPIO Pin to state {}.", deviceName, pin, pinState);
        var lineValues = new LineValues(pinState.getValue(), 1);
        IOCtl.call(fd, Command.getGpioV2SetValuesIoctl(), lineValues);
        this.pinState = pinState;
    }

    /**
     * Checks if direction is explicitly set.
     */
    private void checkDirection() {
        if (pinDirection == null) {
            throw new RuntimeException("Pin " + pin + " direction not set");
        }
    }

    /**
     * Checks if GPIO Pin is closed.
     */
    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("Pin " + pin + " is closed");
        }
    }

    /**
     * Adds default event detection listener with buffer size of 1.
     * WARNING: since the caller of this callback is heavily tight with linux poll, it is recommended to do processing as fast as possible in implementation part.
     * If there is any heavy processing call it is recommended to offload it into different thread.
     *
     * @param pinEvent       the event to detect
     * @param eventProcessor event processor callback
     * @return future to operate the task
     */
    public Future<?> startEventDetection(PinEvent pinEvent, PinEventProcessing eventProcessor) {
        return startEventDetection(pinEvent, eventProcessor, 1);
    }

    /**
     * Adds event detection listener with given event buffer size.
     * When number of events reaches event buffer size, event processor is called.
     * WARNING: since the caller of this callback is heavily tight with linux poll, it is recommended to do processing as fast as possible in implementation part.
     * If there is any heavy processing call it is recommended to offload it into different thread.
     *
     * @param pinEvent        the event to detect
     * @param eventProcessor  event processor callback
     * @param eventBufferSize size of event buffer to be processed
     * @return future to operate the task
     */
    public Future<?> startEventDetection(PinEvent pinEvent, PinEventProcessing eventProcessor, int eventBufferSize) {
        if (watcher.isRunning()) {
            logger.error("{}-{} - cannot start event detection, the watcher thread is already running.", deviceName, pin);
            return null;
        }
        logger.info("{}-{} - adding event {} detection with buffer size {}.", deviceName, pin, pinEvent, eventBufferSize);
        this.watcher = new EventWatcher(fd, pinEvent, eventProcessor, eventBufferSize);
        return eventTaskProcessor.submit(watcher);
    }

    /**
     * Adds event detection listener with given update period.
     * All detected events will be pushed to processing with a period specified in update period. The default event buffer size will be set to 16, according to GPIO default buffer size.
     * WARNING: since the caller of this callback is heavily tight with linux poll, it is recommended to do processing as fast as possible in implementation part.
     * If there is any heavy processing call it is recommended to offload it into different thread.
     *
     * @param pinEvent       the event to detect
     * @param eventProcessor event processor callback
     * @param updatePeriod   update period
     * @return future to operate the task
     */
    public Future<?> startEventDetection(PinEvent pinEvent, PinEventProcessing eventProcessor, Duration updatePeriod) {
        if (watcher.isRunning()) {
            logger.error("{}-{} - cannot start event detection, the watcher thread is already running.", deviceName, pin);
            return null;
        }
        logger.info("{}-{} - adding event {} detection with pulse delay {}.", deviceName, pin, pinEvent, updatePeriod);
        this.watcher = new EventWatcher(fd, pinEvent, eventProcessor, updatePeriod);
        return eventTaskProcessor.submit(watcher);
    }

    /**
     * Stops event detection on pin.
     */
    public void stopEventDetection() {
        this.watcher.stopWatching();
    }

    @Override
    public String toString() {
        return "GPIOPin{" +
                "deviceName='" + deviceName + '\'' +
                ", pin=" + pin +
                ", state=" + pinState +
                ", direction=" + pinDirection +
                ", closed=" + closed +
                '}';
    }

    /**
     * Internal class for watching the event on GPIO Pin.
     */
    private static class EventWatcher implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(EventWatcher.class);

        private final int fd;
        private final PinEvent pinEvent;
        private final PinEventProcessing eventProcessor;
        private final int eventBufferSize;
        private final Duration updatePeriod;

        private boolean stopWatching = false;

        /**
         * Constructs the EventWatcher
         *
         * @param pinEvent        event
         * @param eventProcessor  event processor
         * @param eventBufferSize event buffer size
         */
        EventWatcher(int fd, PinEvent pinEvent, PinEventProcessing eventProcessor, int eventBufferSize) {
            this.fd = fd;
            this.pinEvent = pinEvent;
            this.eventProcessor = eventProcessor;
            this.eventBufferSize = eventBufferSize;
            this.updatePeriod = Duration.ZERO;
        }

        /**
         * Constructs the EventWatcher
         *
         * @param pinEvent       event
         * @param eventProcessor event processor
         * @param updatePeriod   update period
         */
        EventWatcher(int fd, PinEvent pinEvent, PinEventProcessing eventProcessor, Duration updatePeriod) {
            this.fd = fd;
            this.pinEvent = pinEvent;
            this.eventProcessor = eventProcessor;
            this.eventBufferSize = 1;
            this.updatePeriod = updatePeriod;
        }

        @Override
        public void run() {
            var pollFd = new PollingData(fd, (short) (PollFlag.POLLIN | PollFlag.POLLERR), (short) 0);
            var eventSize = (int) LineEvent.LAYOUT.byteSize();
            var timestamp = Instant.now();
            List<DetectedEvent> eventList = new ArrayList<>();
            while (!stopWatching) {
                try {
                    // number of file descriptors is set to 1, since we are polling only one pin
                    // timeout is set to 25s for default
                    var retPollFd = Poll.poll(pollFd, 1, updatePeriod.equals(Duration.ZERO) ? 25_000 : (int) updatePeriod.toMillis());
                    if (retPollFd == null) {
                        // timeout happened, process all left events, update timestamp
                        eventProcessor.process(eventList);
                        eventList.clear();
                        timestamp = Instant.now();
                        continue;
                    }
                    if ((retPollFd.revents() & (PollFlag.POLLIN)) != 0) {
                        // default minimum buffer size is 16 line events
                        // see https://elixir.bootlin.com/linux/latest/source/include/uapi/linux/gpio.h#L185
                        var buf = FileDescriptor.read(fd, 16 * eventSize);
                        var holder = new byte[eventSize];
                        for (int i = 0; i < 16 * LineEvent.LAYOUT.byteSize(); i += eventSize) {
                            // check if timestamp is 0, then there is no event present, we can skip
                            if (buf[i] == 0) {
                                continue;
                            }
                            System.arraycopy(buf, i, holder, 0, eventSize);
                            var memoryBuffer = MemorySegment.ofArray(holder);
                            var event = LineEvent.createEmpty().fromBytes(memoryBuffer);
                            // process only interested events
                            if ((event.id() & this.pinEvent.getValue()) != 0) {
                                eventList.add(new DetectedEvent(event.timestampNs(), PinEvent.getByValue(event.id()), event.lineSeqNo()));
                            }
                        }
                        if (eventList.size() >= eventBufferSize && updatePeriod.equals(Duration.ZERO)) {
                            // process by number of events
                            eventProcessor.process(eventList);
                            eventList.clear();
                        } else if (timestamp.plus(updatePeriod).isBefore(Instant.now())) {
                            // process by update period
                            eventProcessor.process(eventList);
                            eventList.clear();
                            timestamp = Instant.now();
                        }
                    }
                    if ((retPollFd.revents() & (PollFlag.POLLERR)) != 0) {
                        // internal error on polling
                        logger.error("Internal error during polling");
                        stopWatching();
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Stops event watcher, end the task.
         */
        public void stopWatching() {
            this.stopWatching = true;
        }

        /**
         * Checks if the event watcher is running.
         *
         * @return true if event watcher is running
         */
        public boolean isRunning() {
            return !this.stopWatching;
        }

        @Override
        public String toString() {
            return "EventWatcher{" +
                    "fd=" + fd +
                    ", pinEvent=" + pinEvent +
                    ", eventBufferSize=" + eventBufferSize +
                    ", updatePeriod=" + updatePeriod +
                    ", stopWatching=" + stopWatching +
                    '}';
        }
    }
}
