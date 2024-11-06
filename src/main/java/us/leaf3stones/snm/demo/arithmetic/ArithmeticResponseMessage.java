package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.message.Message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ArithmeticResponseMessage extends Message {
    private byte[] message;

    public ArithmeticResponseMessage(byte[] message) {
        this.message = message;
    }

    public ArithmeticResponseMessage(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    protected int getTypeIdentifier() {
        return ArithmeticMessageDecoder.MessageTypeIdentifiers.TYPE_ARITHMETIC_RESPONSE_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        return lengthWithHeader(message);
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        sizedPut(message, buf);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        message = sizedRead(buf);
    }

    public String getMessage() {
        return new String(message, StandardCharsets.UTF_8);
    }

    public static ArithmeticResponseMessage newInstance(String response) {
        return new ArithmeticResponseMessage(response.getBytes(StandardCharsets.UTF_8));
    }
}
