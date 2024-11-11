package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * convenient class when dealing the (de)serialization of complex data structure <br>
 * any class implements this interface should also have a method having this signature
 * <code>public static BsonConvertable fromBson(BSONObject)</code>
 */
public interface BsonConvertable {
    class Helper {
        static <T extends BsonConvertable> BasicBSONList toBsonList(List<T> convertableArray) {
            BasicBSONList list = new BasicBSONList();
            if (convertableArray != null) {
                for (T conv : convertableArray) {
                    list.add(conv.convertToBson());
                }
            }
            return list;
        }

        static <T extends BsonConvertable> List<T> fromBsonList(BasicBSONList list, Method converter) {
            ArrayList<T> arr = new ArrayList<>();
            for (Object convertable : list) {
                try {
                    arr.add((T) converter.invoke(null, convertable));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            return arr;
        }
    }
    BSONObject convertToBson();
}