package org.digitalsmile.gpio.core.file;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;

@NativeMemory
public interface FileDescriptor {
    @Function(name = "open64", useErrno = true, returnType = int.class)
    int open(@ByAddress String path, int openFlag) throws NativeMemoryException;

    @Function(name = "close", useErrno = true)
    void close(int fd)throws NativeMemoryException;

    @Function(name = "read", useErrno = true, returnType = byte[].class)
    byte[] read(int fd, @Returns @ByAddress byte[] buffer, int size) throws NativeMemoryException;

    @Function(name = "write", useErrno = true, returnType = int.class)
    int write(int fd, @ByAddress byte[] data) throws NativeMemoryException;
}
