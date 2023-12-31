package org.ds.io.gpio;

import org.ds.io.core.IOCtl;
import org.ds.io.core.IOFlags;
import org.ds.io.core.NativeCaller;
import org.ds.io.gpio.model.GPIOHandleData;
import org.ds.io.gpio.model.GPIOHandleRequest;
import org.ds.io.gpio.model.GPIOLineInfo;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GPIOPin {
    private static final int FLAG_KERNEL = 1; // 1
    private static final int FLAG_IS_OUT = 1 << 1; // 2
    private static final int FLAG_ACTIVE_LOW = 1 << 2; // 4
    private static final int FLAG_OPEN_DRAIN = 1 << 3; // 8
    private static final int FLAG_OPEN_SOURCE = 1 << 4; // 16
    private static final int FLAG_BIAS_PULL_UP = 1 << 5; // 32
    private static final int FLAG_BIAS_PULL_DOWN = 1 << 6; // 64
    private static final int FLAG_BIAS_DISABLE = 1 << 7; // 128

    private final String deviceName;
    private final int pin;
    private final GPIOLineInfo gpioLineInfo;
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());


    private int fd;
    private GPIOState state;
    private GPIODirection direction;
    private boolean closed = false;

    GPIOPin(String deviceName, int gpioPin, GPIODirection direction) throws IOException {
        this.deviceName = deviceName;
        this.pin = gpioPin;

        this.fd = NativeCaller.open(deviceName, IOFlags.O_RDONLY | IOFlags.O_CLOEXEC);
        var data = new GPIOLineInfo(gpioPin, 0, new byte[]{}, new byte[]{});
        this.gpioLineInfo = NativeCaller.ioctl(fd, IOCtl.getGpioGetLineInfoIoctl(), data);
        if ((gpioLineInfo.flags() & FLAG_KERNEL) > 0) {
            NativeCaller.close(fd);
            throw new IOException("Pin " + pin + " is blocked by Kernel");
        }
        setDirection(direction);
    }

    public String getName() {
        return new String(gpioLineInfo.name());
    }

    public int getPin() {
        return pin;
    }

    public int getFd() {
        return fd;
    }

    public GPIOState getState() {
        return state;
    }

    public void setState(GPIOState state) {
        this.state = state;
    }

    public GPIODirection getDirection() {
        return direction;
    }

    public void setDirection(GPIODirection direction) throws IOException {
        checkClosed();
        if (direction.equals(this.direction)) {
            return;
        }
        var label = "e-ink-display".getBytes();
        var gpioHandleRequest = new GPIOHandleRequest(new int[]{pin}, direction.getMode(), new byte[]{}, label, 1, 0);
        var result = NativeCaller.ioctl(fd, IOCtl.getGpioGetLineHandleIoctl(), gpioHandleRequest);
        this.fd = result.fd();
        this.direction = direction;
    }

    public GPIOState read() throws IOException {
        checkClosed();
        checkDirection();
        if (GPIODirection.OUTPUT.equals(this.direction)) {
            throw new IOException("Can't read from output pin " + new String(gpioLineInfo.name()) + ". The direction is set to output");
        }

        var gpioHandleData = new GPIOHandleData(new byte[1]);
        var result = NativeCaller.ioctl(fd, IOCtl.getGpioHandleGetLineValuesIoctl(), gpioHandleData);

        return result.values()[0] == 1 ? GPIOState.HIGH : GPIOState.LOW;
    }

    public void write(GPIOState state) throws IOException {
        checkClosed();
        checkDirection();
        if (GPIODirection.INPUT.equals(this.direction)) {
            throw new IOException("Can't write to input pin " + new String(gpioLineInfo.name()) + ". The direction is set to input.");
        }

        var gpioHandleData = new GPIOHandleData(new byte[]{state.getState()});
        NativeCaller.ioctl(fd, IOCtl.getGpioHandleSetLineValuesIoctl(), gpioHandleData);
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

    private GPIOState callbackResult;
    public ScheduledFuture<?> addEventDetection(GPIOEvent event, GPIOEventCallback eventCallback) {
        if (event.equals(GPIOEvent.FALLING)) {
            callbackResult = GPIOState.HIGH;
        } else if (event.equals(GPIOEvent.RISING)) {
            callbackResult = GPIOState.LOW;
        } else {
            callbackResult = GPIOState.LOW;
        }
        return executorService.scheduleAtFixedRate(() -> {
            try {
                var result = read();
                switch (event) {
                    case RISING -> {
                        if (result.equals(GPIOState.HIGH) && callbackResult.equals(GPIOState.LOW)) {
                            //System.out.println("DETECTED RISING!");
                            eventCallback.eventDetected(result);
                        }
                    }
                    case FALLING -> {
                        if (result.equals(GPIOState.LOW) && callbackResult.equals(GPIOState.HIGH)) {
                            //System.out.println("DETECTED FALLING!");
                            eventCallback.eventDetected(result);
                        }
                    }
                    case BOTH -> {
                        if (!callbackResult.equals(result)) {
                            //System.out.println("DETECTED ANYTHING! " + result);
                            eventCallback.eventDetected(result);
                        }
                    }
                }
                callbackResult = result;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }

        }, 0, 10, TimeUnit.NANOSECONDS);
    }
}
