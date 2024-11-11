package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public abstract class BsonMessage extends Message {
    private Method backConverter;

    private BSONObject data;
    private byte[] cachedSerialization;

    public BsonMessage(BSONObject object) {
        data = object;
    }

    public BsonMessage(ByteBuffer buffer, Method backConverter) {
        super(buffer);
        this.backConverter = backConverter;
    }

    @Override
    protected int peekDataSize() {
        cachedSerialization = new BasicBSONEncoder().encode(data);
        return lengthWithHeader(cachedSerialization);
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        sizedPut(cachedSerialization, buf);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        data = new BasicBSONDecoder().readObject(sizedRead(buf));
    }

    public static BsonMessage fromConvertable(BsonConvertable convertable, int messageId) {
        return new BsonMessage(convertable.convertToBson()) {
            @Override
            protected int getTypeIdentifier() {
                return messageId;
            }
        };
    }

    public BsonConvertable toConvertable() {
        try {
            return (BsonConvertable) backConverter.invoke(null, data);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
