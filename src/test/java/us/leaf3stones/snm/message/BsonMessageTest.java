package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.jupiter.api.Test;
import us.leaf3stones.snm.crypto.NativeBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BsonMessageTest {
    static class ExampleBsonConvertable implements BsonConvertable {
        private String stringData;
        private int intData;
        private double doubleData;
        private byte[] byteArrayData;

        public ExampleBsonConvertable(String stringData, int intData, double doubleData, byte[] byteArrayData) {
            this.stringData = stringData;
            this.intData = intData;
            this.doubleData = doubleData;
            this.byteArrayData = byteArrayData;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExampleBsonConvertable that = (ExampleBsonConvertable) o;
            return intData == that.intData && Double.compare(doubleData, that.doubleData) == 0 && Objects.equals(stringData, that.stringData) && Objects.deepEquals(byteArrayData, that.byteArrayData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stringData, intData, doubleData, Arrays.hashCode(byteArrayData));
        }

        @Override
        public BSONObject convertToBson() {
            BSONObject obj = new BasicBSONObject();
            obj.put("str", stringData);
            obj.put("int", intData);
            obj.put("double", doubleData);
            obj.put("byteArray", byteArrayData);
            return obj;
        }

        public static ExampleBsonConvertable fromBson(BSONObject obj) {
            String str = (String) obj.get("str");
            int intValue = (int) obj.get("int");
            double doubleValue = (double) obj.get("double");
            byte[] ba = (byte[]) obj.get("byteArray");
            return new ExampleBsonConvertable(str, intValue, doubleValue, ba);
        }
    }

    static class ListBsonConvertable implements BsonConvertable {
        static boolean called = false;

        @Override
        public BSONObject convertToBson() {
            ArrayList<ExampleBsonConvertable> list = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                list.add(new ExampleBsonConvertable("str", 5, 10.0, new byte[i]));
            }
            BSONObject bo = new BasicBSONObject();
            bo.put("data", 3);
            bo.put("list", Helper.toBsonList(list));
            return bo;
        }

        public static ListBsonConvertable fromBson(BSONObject bo) {
            assertEquals(3, bo.get("data"));
            List<ExampleBsonConvertable> list = BsonConvertable.Helper.fromBsonList((BasicBSONList) bo.get("list"), BsonDecoder.getConverter(1005));
            assertEquals(3, list.size());
            for (int i = 0; i < 3; ++i ) {
                ExampleBsonConvertable convertable = list.get(i);
                assertEquals("str", convertable.stringData);
                assertEquals(5, convertable.intData);
                assertEquals(10.0, convertable.doubleData);
                assertEquals(i, convertable.byteArrayData.length);
            }
            called = true;
            return new ListBsonConvertable();
        }

    }

    static class ExampleBsonMessage extends BsonMessage {
        public ExampleBsonMessage(BSONObject object) {
            super(object);
        }

        public ExampleBsonMessage(ByteBuffer buffer) {
            super(buffer);
        }

        @Override
        protected int getTypeIdentifier() {
            return 1005;
        }
    }

    static class ListBsonMessage extends BsonMessage {

        public ListBsonMessage(BSONObject object) {
            super(object);
        }

        public ListBsonMessage(ByteBuffer buffer) {
            super(buffer);
        }

        @Override
        protected int getTypeIdentifier() {
            return 1006;
        }
    }

    @Test
    void backAndForth() {
        ExampleBsonConvertable bc = new ExampleBsonConvertable("3", 1, 3.0, new byte[3]);
        Message bm = BsonMessage.fromConvertable(bc, 1005);
        NativeBuffer buffer = bm.serialize();
        byte[] arr = new byte[(int) buffer.size()];
        buffer.wrapAsByteBuffer().get(arr);
        buffer.clean();

        BsonDecoder.registerBsonMessage(ExampleBsonConvertable.class, ExampleBsonMessage.class, 1005);
        ExampleBsonMessage msg = (ExampleBsonMessage) new BsonDecoder(null).decode(arr);
        ExampleBsonConvertable recoveredConvertable = (ExampleBsonConvertable) msg.toConvertable();
        assertEquals(bc, recoveredConvertable);
    }

    @Test
    void complexArray() {
        BsonConvertable conv = new ListBsonConvertable();
        Message bm = BsonMessage.fromConvertable(conv, 1006);
        NativeBuffer buffer = bm.serialize();
        byte[] arr = new byte[(int) buffer.size()];
        buffer.wrapAsByteBuffer().get(arr);
        buffer.clean();

        BsonDecoder.registerBsonMessage(ExampleBsonConvertable.class, ExampleBsonMessage.class, 1005);
        BsonDecoder.registerBsonMessage(ListBsonConvertable.class, ListBsonMessage.class,1006);

        ListBsonMessage msg = (ListBsonMessage) new BsonDecoder(null).decode(arr);
        msg.toConvertable();
        assertTrue(ListBsonConvertable.called);

    }
}
