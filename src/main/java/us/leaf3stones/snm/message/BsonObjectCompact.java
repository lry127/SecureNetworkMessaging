package us.leaf3stones.snm.message;

import org.bson.BSONObject;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class BsonObjectCompact {
    private final BSONObject obj;

    public BsonObjectCompact(BSONObject obj) {
        this.obj = obj;
    }

    public BSONObject getBsonObject() {
        return obj;
    }

    public <T> T get(String key) {
        return (T) obj.get(key);
    }

    public <T> T requireObject(String key) {
        Object data = obj.get(key);
        if (data == null) {
            throw new RuntimeException("failed to get object with key: " + key);
        }
        return (T) data;
    }

    public <T> T getObjectOrElse(String key, T defaultValue) {
        Object data = obj.get(key);
        if (data == null) {
            data = defaultValue;
        }
        return (T) data;
    }

    public <T> T getObjectOrElseGet(String key, Supplier<T> supplier) {
        Object data = obj.get(key);
        if (data == null) {
            data = supplier.get();
        }
        return (T) data;
    }

}
