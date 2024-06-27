package org.digitalsmile.gpio.core.poll;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;

@NativeMemory(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/asm-generic/poll.h")
@Structs({
        @Struct(name = "pollfd", javaName = "PollingData")
})
public interface Poll {

    @Function(name = "poll", useErrno = true, returnType = int.class)
    PollingData poll(@Returns PollingData pollingData, int size, int timeout) throws NativeMemoryException;
}
