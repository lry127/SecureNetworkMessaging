package us.leaf3stones.snm.message;

import us.leaf3stones.snm.crypto.NativeBuffer;

import java.nio.ByteBuffer;

public abstract class Message {
    public Message(ByteBuffer buffer) {
        try {
            constructMessage(buffer);
        } catch (Exception e) {
            throw new MessageParseException(e);
        }
    }

    public Message() {

    }

    protected static int lengthWithHeader(byte[] bytes) {
        return Integer.BYTES + bytes.length;
    }

    protected static void sizedPut(byte[] bytes, ByteBuffer buffer) {
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    protected static byte[] sizedRead(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] result = new byte[length];
        buffer.get(result);
        return result;
    }

    NativeBuffer serialize() {
        NativeBuffer nativeBuffer = new NativeBuffer(Integer.BYTES + peekDataSize());
        try {
            ByteBuffer wrapped = nativeBuffer.wrapAsByteBuffer();
            wrapped.putInt(getTypeIdentifier());
            serialize(wrapped);
        } catch (Exception e) {
            nativeBuffer.clean();
            throw new RuntimeException("failed to serialize: " + e.getMessage());
        }
        return nativeBuffer;
    }

    protected abstract int getTypeIdentifier();

    protected abstract int peekDataSize();

    protected abstract void serialize(ByteBuffer buf);

    protected abstract void constructMessage(ByteBuffer buf) throws Exception;

    public static class MessageParseException extends RuntimeException {
        public MessageParseException(Throwable cause) {
            super(cause);
        }
    }


}
