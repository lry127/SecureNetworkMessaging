package us.leaf3stones.snm.message;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 2 + peekDataSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.putInt(buffer.capacity() - Integer.BYTES); // how many bytes remaining?
            buffer.putInt(getTypeIdentifier());
            serialize(buffer);
        } catch (Exception e) {
            throw new RuntimeException("failed to serialize: " + e.getMessage());
        }
        return buffer.array();
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
