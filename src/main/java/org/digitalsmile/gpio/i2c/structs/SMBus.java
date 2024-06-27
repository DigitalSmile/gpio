package org.digitalsmile.gpio.i2c.structs;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.structure.Union;
import io.github.digitalsmile.annotation.structure.Unions;

@NativeMemory(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/i2c.h")
@Unions({
        @Union(name = "i2c_smbus_data", javaName = "SMBusData")
})
public interface SMBus {
}
