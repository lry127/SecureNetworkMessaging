package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;

public class AuthenticationMessage extends Message {
    private long base;

    AuthenticationMessage(ByteBuffer buffer) {
        super(buffer);
    }

    AuthenticationMessage(long base) {
        this.base = base;
    }

    public static AuthenticationMessage newInstance(long base) {
        return new AuthenticationMessage(base);
    }

    @Override
    public int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_AUTHENTICATION_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return Long.BYTES;
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        buf.putLong(base);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        base = buf.getLong();
    }

    public long getBase() {
        return base;
    }
}
