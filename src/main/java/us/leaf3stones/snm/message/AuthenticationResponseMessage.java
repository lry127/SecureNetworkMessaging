package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;

public class AuthenticationResponseMessage extends Message {
    private byte isBypass;
    private byte[] response;

    AuthenticationResponseMessage(ByteBuffer buffer) {
        super(buffer);
    }

    private AuthenticationResponseMessage(byte isBypass, byte[] response) {
        this.isBypass = isBypass;
        this.response = response;
    }

    public static AuthenticationResponseMessage newInstance(boolean isBypass, byte[] response) {
        byte bypassByte = (byte) (isBypass ? 0x1 : 0x0);
        return new AuthenticationResponseMessage(bypassByte, response);
    }

    public static AuthenticationResponseMessage newInstance(boolean isBypass, long response) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(response);
        return newInstance(isBypass, buf.array());
    }

    @Override
    int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_AUTHENTICATION_RESPONSE_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return Byte.BYTES + lengthWithHeader(response);
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        buf.put(isBypass);
        sizedPut(response, buf);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        isBypass = buf.get();
        response = sizedRead(buf);
    }

    public long getResponseAsLong() {
        return ByteBuffer.wrap(getResponse()).getLong();
    }

    public byte[] getResponse() {
        return response;
    }

    public boolean isBypass() {
        return isBypass != 0x0;
    }
}
