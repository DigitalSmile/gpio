[![](https://jitpack.io/v/DigitalSmile/gpio.svg)](https://jitpack.io/#DigitalSmile/gpio)
![](https://img.shields.io/badge/Java-21+-success)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/digitalsmile/gpio/gradle.yml)
---
# Java GPIO library with new java FFM API
With new FFM API that were released in a recent versions of Java you can work with any kind of hardware just within your app. No more JNI and JNA!

Read more about FFM in here -> https://openjdk.org/jeps/442

Since Java 22 it will be the default option for usage the external C libraries. Be prepared now!

## Features
1) Zero dependencies (except SLF4J for logging)
2) Modern Java 21+ with full language feature support
3) Supports working with individual GPIO Pins
4) Supports working with SPI
5) Tested on Raspberry Pi 4.
6) i2c and UART/TTL coming soon.

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
2) <b>IMPORTANT:</b> add Java VM Option `--enable-preview` in your IDE and gradle -> https://stackoverflow.com/questions/72083752/enable-preview-features-in-an-early-access-version-of-java-in-intellij (that will go away when JAva will be upgraded to 22)
3) Add to your code:

```java
var spiBus = GPIOBoard.ofSPI(0, SPIMode.MODE_0, 20_000_000);

var rst = GPIOBoard.ofPin(17, Direction.OUTPUT);
var busy = GPIOBoard.ofPin(24, Direction.INPUT);
var dc = GPIOBoard.ofPin(25, Direction.OUTPUT);
var pwr = GPIOBoard.ofPin(18, Direction.OUTPUT);

pwr.write(State.HIGH);
while (busy.read().equals(State.HIGH)) {
    Thread.sleep(10);
}

dc.write(State.LOW);
spiBus.sendByteData(new byte[]{1}, false);
```
4) Enjoy! :)