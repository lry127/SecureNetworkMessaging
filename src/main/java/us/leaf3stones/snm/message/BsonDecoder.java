package us.leaf3stones.snm.message;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BsonDecoder extends MessageDecoder {
    private static final Map<Integer, Constructor<? extends BsonConvertable>> registeredBsonConverters = new HashMap<>();
    private static final Map<Integer, Constructor<? extends BsonMessage<?>>> registeredBsonMessageConstructors = new HashMap<>();

    public static void registerBsonMessage(Class<? extends BsonConvertable> convertableClass,
                                           Class<? extends BsonMessage<?>> messageClass, int messageId) {
        try {
            Constructor<? extends BsonConvertable> converter = convertableClass.getConstructor();
            Constructor<? extends BsonMessage<?>> constructor = messageClass.getConstructor(ByteBuffer.class);
            registeredBsonConverters.put(messageId, converter);
            registeredBsonMessageConstructors.put(messageId, constructor);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Constructor<?> getConstructor(int messageId) {
        return registeredBsonConverters.get(messageId);
    }

    public BsonDecoder(MessageDecoder parent) {
        super(parent);
    }

    @Override
    protected Message convert(int messageId, ByteBuffer messageBody) throws DecodeException {
        if (registeredBsonConverters.containsKey(messageId)) {
            Constructor<?> converter = registeredBsonConverters.get(messageId);
            Constructor<? extends BsonMessage<?>> messageConstructor = registeredBsonMessageConstructors.get(messageId);
            try {
                BsonMessage<?> bsonMsg = messageConstructor.newInstance(messageBody);
                bsonMsg.setBackConverter(converter);
                return bsonMsg;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new DecodeException(e);
            }
        } else {
            throw new RuntimeException("can't decode this message with id: " + messageId);
        }
    }

    @Override
    protected Set<Integer> getConvertableMessageIds() {
        return registeredBsonConverters.keySet();
    }
}
