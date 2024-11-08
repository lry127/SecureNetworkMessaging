package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.MessageDecoder;

import java.nio.ByteBuffer;
import java.util.Set;

public class ArithmeticMessageDecoder extends MessageDecoder {
    public ArithmeticMessageDecoder(MessageDecoder parent) {
        super(parent);
    }

    @Override
    protected Message convert(int messageId, ByteBuffer messageBody) throws DecodeException {
        if (messageId == MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE) {
            return new ArithmeticMessage(messageBody);
        } else if (messageId == MessageTypeIdentifiers.TYPE_ARITHMETIC_RESPONSE_MESSAGE) {
            return new ArithmeticResponseMessage(messageBody);
        }
        throw new AssertionError("can't go here");
    }

    @Override
    protected Set<Integer> getConvertableMessageIds() {
        return Set.of(MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE,
                MessageTypeIdentifiers.TYPE_ARITHMETIC_RESPONSE_MESSAGE);
    }

    public static class MessageTypeIdentifiers {
        public static int TYPE_ARITHMETIC_MESSAGE = 1000;
        public static int TYPE_ARITHMETIC_RESPONSE_MESSAGE = 1001;
    }
}
