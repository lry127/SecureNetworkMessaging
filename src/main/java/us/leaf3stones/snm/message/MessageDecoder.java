package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;

public abstract class MessageDecoder {
    private final MessageDecoder parent;
    private final Set<Integer> convertableMessages;

    public MessageDecoder(MessageDecoder parent) {
        this.parent = parent;
        convertableMessages = Collections.unmodifiableSet(getConvertableMessageIds());
    }

    public final Message decode(byte[] message) throws DecodeException {
        if (message == null || message.length < Integer.BYTES) {
            throw new DecodeException("message too short or null.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(message);
        int type = buffer.getInt();

        MessageDecoder decoder = this;
        while (true) {
            if (decoder.convertableMessages.contains(type)) {
                return decoder.convert(type, buffer);
            }
            if (decoder.parent == null) {
                throw new DecodeException("unknown typeId: " + type);
            } else {
                decoder = decoder.parent;
            }
        }
    }

    protected abstract Message convert(int messageId, ByteBuffer messageBody) throws DecodeException;

    protected abstract Set<Integer> getConvertableMessageIds();

    public static class DecodeException extends RuntimeException {
        protected DecodeException(String message) {
            super(message);
        }

        protected DecodeException(Exception e) {
            super(e);
        }
    }
}
