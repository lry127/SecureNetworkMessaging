package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ResponseMessage extends Message {
    private byte ok;
    private byte[] message;

    ResponseMessage(ByteBuffer buffer) {
        super(buffer);
    }

    ResponseMessage(byte ok, byte[] message) {
        super();
        this.ok = ok;
        this.message = message;
    }

    public static ResponseMessage newInstance(boolean isOk, String message) {
        byte ok;
        if (isOk) {
            ok = 1;
        } else {
            ok = 0;
        }
        return new ResponseMessage(ok, message.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isOk() {
        return ok != 0;
    }

    public String getMessage() {
        return new String(message, StandardCharsets.UTF_8);
    }

    @Override
    int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_RESPONSE_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return Byte.BYTES + lengthWithHeader(message);
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        buf.put(ok);
        sizedPut(message, buf);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        ok = buf.get();
        message = sizedRead(buf);
    }
}
