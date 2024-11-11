package us.leaf3stones.snm.message;

import org.bson.BSONObject;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BsonDecoder extends MessageDecoder {
    private static final Map<Integer, Method> registeredBsonMessages = new HashMap<>();

    public static void registerBsonMessage(Class<? extends BsonConvertable> messageClass, int messageId) {
        try {
            registeredBsonMessages.put(messageId, messageClass.getMethod("fromBson", BSONObject.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public BsonDecoder(MessageDecoder parent) {
        super(parent);
    }

    @Override
    protected Message convert(int messageId, ByteBuffer messageBody) throws DecodeException {
        if (registeredBsonMessages.containsKey(messageId)) {
            return new BsonMessage(messageBody, registeredBsonMessages.get(messageId)) {
                @Override
                protected int getTypeIdentifier() {
                    return messageId;
                }
            };
        }
        throw new RuntimeException("can't decode this message with id: " + messageId);
    }

    @Override
    protected Set<Integer> getConvertableMessageIds() {
        return registeredBsonMessages.keySet();
    }
}
