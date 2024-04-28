/**
 * Main GPIO module
 */
module gpio.main {
    requires org.slf4j;

    exports org.digitalsmile.gpio;
    exports org.digitalsmile.gpio.pin;
    exports org.digitalsmile.gpio.pin.attributes;
    exports org.digitalsmile.gpio.pin.event;
    exports org.digitalsmile.gpio.spi;
    exports org.digitalsmile.gpio.spi.attributes;
    exports org.digitalsmile.gpio.i2c;
    exports org.digitalsmile.gpio.i2c.attributes;
    exports org.digitalsmile.gpio.pwm;
    exports org.digitalsmile.gpio.pwm.attributes;
}