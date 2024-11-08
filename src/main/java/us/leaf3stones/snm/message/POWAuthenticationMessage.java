package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;

public class POWAuthenticationMessage extends AuthenticationMessage {
    private short specification;
    private short minBypassMillis;

    POWAuthenticationMessage(ByteBuffer buffer) {
        super(buffer);
    }

    POWAuthenticationMessage(short specification, short minBypassMillis, long base) {
        super(base);
        this.specification = specification;
        this.minBypassMillis = minBypassMillis;
    }

    @Override
    public int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_POW_AUTHENTICATION_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return super.peekDataSize() + Short.BYTES * 2;
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        super.serialize(buf);
        buf.putShort(specification);
        buf.putShort(minBypassMillis);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        super.constructMessage(buf);
        specification = buf.getShort();
        minBypassMillis = buf.getShort();
    }

    public short getSpecification() {
        return specification;
    }

    public short getMinBypassMillis() {
        return minBypassMillis;
    }



    public static POWAuthenticationMessage newInstance(short specification, short minBypassMillis, long base) {
        return new POWAuthenticationMessage(specification, minBypassMillis, base);
    }

}
