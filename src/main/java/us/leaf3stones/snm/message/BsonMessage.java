package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.BasicBSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public abstract class BsonMessage<T extends BsonConvertable> extends Message {
    private Constructor<T> constructor;

    private BSONObject data;
    private byte[] cachedSerialization;

    public BsonMessage(T convertable) {
        data = new BasicBSONObject();
        convertable.convertToBson(data);
    }

    public BsonMessage(ByteBuffer buffer) {
        super(buffer);
    }

    @SuppressWarnings("unchecked")
    void setBackConverter(Constructor<?> constructor) {
        this.constructor = (Constructor<T>) constructor;
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


    public T convertBack() {
        try {
            T instance = constructor.newInstance();
            instance.fromBson(new BsonObjectCompact(data));
            return instance;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
