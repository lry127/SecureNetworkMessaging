package us.leaf3stones.snm.message;

import org.bson.BSONObject;

/**
 * convenient class when dealing the (de)serialization of complex data structure <br>
 * any class implements this interface should also have a method having this signature
 * <code>public static BsonConvertable fromBson(BSONObject)</code>
 */
public interface BsonConvertable {
    BSONObject convertToBson();
}
