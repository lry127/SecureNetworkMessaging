package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class GeneralFailureResponse extends Message {
    private byte[] payload;

    GeneralFailureResponse(byte[] payload) {
        this.payload = payload;
    }

    public GeneralFailureResponse(String message) {
        this(message.getBytes(StandardCharsets.UTF_8));
    }

    public GeneralFailureResponse(ByteBuffer buffer) {
        super(buffer);
    }

    public String getPayloadAsString() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    protected int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_GENERAL_FAILURE_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return lengthWithHeader(payload);
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        sizedPut(payload, buf);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        payload = sizedRead(buf);
    }
}
