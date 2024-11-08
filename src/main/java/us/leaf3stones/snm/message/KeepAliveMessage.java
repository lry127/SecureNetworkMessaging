package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;

public class KeepAliveMessage extends Message {
    public KeepAliveMessage(ByteBuffer buffer) {
        super(buffer);
    }

    public KeepAliveMessage() {

    }

    public static KeepAliveMessage newInstance() {
        return new KeepAliveMessage();
    }

    @Override
    protected int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_KEEP_ALIVE;
    }

    @Override
    protected int peekDataSize() {
        return Byte.BYTES;
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        buf.put((byte) 0x00);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        buf.get();
    }
}
