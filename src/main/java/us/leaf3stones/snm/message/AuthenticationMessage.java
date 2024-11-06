package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;

public class AuthenticationMessage extends Message {
    private short specification;
    private short minBypassMillis;
    private long base;

    AuthenticationMessage(ByteBuffer buffer) {
        super(buffer);
    }

    private AuthenticationMessage(short specification, short minBypassMillis, long base) {
        this.specification = specification;
        this.minBypassMillis = minBypassMillis;
        this.base = base;
    }

    public static AuthenticationMessage newInstance(short specification, short minBypassMillis, long base) {
        return new AuthenticationMessage(specification, minBypassMillis, base);
    }

    @Override
    int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_AUTHENTICATION_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return Short.BYTES * 2 + Long.BYTES;
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        buf.putShort(specification);
        buf.putShort(minBypassMillis);
        buf.putLong(base);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        specification = buf.getShort();
        minBypassMillis = buf.getShort();
        base = buf.getLong();
    }

    public short getSpecification() {
        return specification;
    }

    public short getMinBypassMillis() {
        return minBypassMillis;
    }

    public long getBase() {
        return base;
    }
}
