package org.digitalsmile.gpio.core.ioctl;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;

@NativeMemory
public interface Ioctl {
    @Function(name = "ioctl", useErrno = true, returnType = int.class)
    int callByValue(int fd, long command, long data) throws NativeMemoryException;

    @Function(name = "ioctl", useErrno = true, returnType = int.class)
    long call(int fd, long command, @Returns @ByAddress long data) throws NativeMemoryException;

    @Function(name = "ioctl", useErrno = true, returnType = int.class)
    int call(int fd, long command, @Returns @ByAddress int data)  throws NativeMemoryException;

    @Function(name = "ioctl", useErrno = true, returnType = int.class)
    <T extends NativeMemoryLayout> T call(int fd, long command, @Returns T data) throws NativeMemoryException;
}
