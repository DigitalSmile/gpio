[![](https://jitpack.io/v/DigitalSmile/gpio.svg)](https://jitpack.io/#DigitalSmile/gpio)
![](https://img.shields.io/badge/Java-22+-success)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/digitalsmile/gpio/gradle.yml)
---
# Java GPIO library
This Java library is intended to be used with any kind of hardware, that exposes GPIO interface to user (e.g. RaspberryPi, OrangePi, etc.). 
With new [FFM API](https://openjdk.org/jeps/442) that were released in a recent versions of Java, you can work with low level stuff (like linux kernel syscalls) within your app. It is fast and reliable native interface provided by JVM. No more JNI and JNA!

## Features
1) Zero dependencies (except SLF4J for logging)
2) Modern! Java 22+ with full language feature support
3) Full usage of FFM API, direct work with native memory and linux syscalls (libc)
4) Supports individual GPIO Pins, with edge event detection
5) Supports SPI interface
6) Supports I2C / SMBus interface
7) Supports hardware PWM interface
8) Tested on RaspberryPi 3/4, OrangePi,
9) UART/TTL is coming soon

## Usage
1) Add a dependency.
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.DigitalSmile:gpio:{version}'
}
```
2) Add to your code:

```java
var spiBus = GPIOBoard.ofSPI(0, SPIMode.MODE_0, 20_000_000);

var rst = GPIOBoard.ofPin(17, Direction.OUTPUT);
var busy = GPIOBoard.ofPin(24, Direction.INPUT);
var dc = GPIOBoard.ofPin(25, Direction.OUTPUT);
var pwr = GPIOBoard.ofPin(18, Direction.OUTPUT);

pwr.write(State.HIGH);
// direct lookup for state change
while(busy.read().equals(State.HIGH)){
        Thread.sleep(10);
}
// or usage of edge event detection
pwr.startEventDetection(PinEvent.FALLING, (eventList ->{
    // do stuff on events captured            
}));
dc.write(State.LOW);

spiBus.sendByteData(new byte[] { 1 },false);

var i2c = GPIOBoard.ofI2C(0);
var scanned = i2c.scan();
var status = scanned.get(0x55);
if (status.equals(I2CStatus.AVAILABLE)) {
    i2c.selectAddress(0x55);
    var read = i2c.read(0x55);
    //do stuff with data read
}

var pwmPin = GPIOBoard.ofPWMBus(0);
pwmPin.configure(25_000, 40, PWMPolarity.NORMAL);
pwmPin.enable();
// set rotation speed, e.g. fan
var percentages = new int[]{100, 90, 80, 70, 60, 50, 40, 30, 20, 10, 0};
for (int percent : percentages) {
    pwmPin.setSpeed(percent);
    Thread.sleep(1_000);
}
```
4) Enjoy! :)