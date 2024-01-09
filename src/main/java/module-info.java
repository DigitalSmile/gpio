/**
 * Main GPIO module
 */
module gpio.main {
    requires org.slf4j;

    exports org.digitalsmile.gpio.core;
    exports org.digitalsmile.gpio.pin;
    exports org.digitalsmile.gpio.pin.attributes;
    exports org.digitalsmile.gpio.spi;
    exports org.digitalsmile.gpio.pin.event;
    exports org.digitalsmile.gpio;
}