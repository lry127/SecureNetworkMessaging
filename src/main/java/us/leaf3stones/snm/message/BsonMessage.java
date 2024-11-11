package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public abstract class BsonMessage<T extends BsonConvertable> extends Message {
    private Method backConverter;

    private BSONObject data;
    private byte[] cachedSerialization;

    public BsonMessage(T convertable) {
        data = convertable.convertToBson();
    }

    public BsonMessage(ByteBuffer buffer) {
        super(buffer);
    }

    void setBackConverter(Method backConverter) {
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


    public T convertBack() {
        try {
            return (T) backConverter.invoke(null, data);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
