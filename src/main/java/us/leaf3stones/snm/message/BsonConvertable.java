package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * convenient class when dealing the (de)serialization of complex data structure <br>
 * any class implements this interface should also have a method having this signature
 * <code>public static BsonConvertable fromBson(BSONObject)</code>
 */
public interface BsonConvertable {
    class Helper {
        public static <T extends BsonConvertable> BasicBSONList toBsonList(List<T> convertableArray) {
            BasicBSONList list = new BasicBSONList();
            if (convertableArray != null) {
                for (T conv : convertableArray) {
                    BSONObject bson = new BasicBSONObject();
                    conv.convertToBson(bson);
                    list.add(bson);
                }
            }
            return list;
        }

        @SuppressWarnings("unchecked")
        public static <T extends BsonConvertable> List<T> fromBsonList(BasicBSONList list, int listItemMessageId) {
            ArrayList<T> arr = new ArrayList<>();
            for (Object convertable : list) {
                try {
                    Constructor<T> itemConstructor = (Constructor<T>) BsonDecoder.getConstructor(listItemMessageId);
                    T instance = itemConstructor.newInstance();
                    instance.fromBson(new BsonObjectCompact((BSONObject) convertable));
                    arr.add(instance);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
            return arr;
        }
    }

    void fromBson(BsonObjectCompact bson);
    void convertToBson(BSONObject bson);
}