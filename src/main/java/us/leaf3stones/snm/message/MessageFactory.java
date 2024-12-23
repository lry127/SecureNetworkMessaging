package us.leaf3stones.snm.message;

import us.leaf3stones.snm.common.NetIOException;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MessageFactory {
    private static final int MAX_ACCEPTABLE_MESSAGE_LENGTH = 65535;
    private final MessageDecoder decoder;

    public MessageFactory(MessageDecoder decoder) {
        this.decoder = decoder;
    }

    public Message parseMessage(InputStream in) throws NetIOException {
        int length = ByteBuffer.wrap(InputStreamUtil.readNBytes(in, Integer.BYTES)).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length < 0 || length > MAX_ACCEPTABLE_MESSAGE_LENGTH) {
            throw new NetIOException("unexpected message length: " + length, true);
        }
        return decoder.decode(InputStreamUtil.readNBytes(in, length));
    }

    public byte[] serializeMessage(Message message) {
        byte[] serialized = message.serialize();
        if (serialized.length > MAX_ACCEPTABLE_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message too large for serialization. consider slicing it");
        }
        return serialized;
    }
}
