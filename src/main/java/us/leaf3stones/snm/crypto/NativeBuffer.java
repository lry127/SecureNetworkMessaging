package us.leaf3stones.snm.crypto;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class NativeBuffer {
    private static final Unsafe theUnsafe;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            theUnsafe = (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final long size;
    private long handle;

    public NativeBuffer(long bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("buffer size must be greater than zero");
        }
        handle = theUnsafe.allocateMemory(bufferSize);
        if (handle == 0) {
            throw new RuntimeException("unable to allocate memory of this size: " + bufferSize);
        }
        size = bufferSize;
    }

    private static native ByteBuffer wrapAsByteBuffer(long addr, long size);

    public ByteBuffer wrapAsByteBuffer() {
        if (handle == 0) {
            throw new IllegalStateException("this buffer no longer valid. is clean() called?");
        }
        return wrapAsByteBuffer(handle, size);
    }

    public long getHandle() {
        return handle;
    }

    public long size() {
        return size;
    }

    public void clean() {
        if (handle == 0) {
            return;
        }
        theUnsafe.freeMemory(handle);
        handle = 0;
    }

}
