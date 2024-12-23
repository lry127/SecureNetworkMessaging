package us.leaf3stones.snm.message;

import org.bson.BSONObject;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BsonMessageTest {
    @Test
    void backAndForth() {
        ExampleBsonConvertable bc = new ExampleBsonConvertable("3", 1, 3.0, new byte[3]);
        Message bm = new ExampleBsonMessage(bc);
        byte[] buffer = bm.serialize();
        byte[] arr = new byte[buffer.length - Integer.BYTES];
        System.arraycopy(buffer, 4, arr, 0, arr.length);

        BsonDecoder.registerBsonMessage(ExampleBsonConvertable.class, ExampleBsonMessage.class, 1005);
        ExampleBsonMessage msg = (ExampleBsonMessage) new BsonDecoder(null).decode(arr);
        ExampleBsonConvertable recoveredConvertable = msg.convertBack();
        assertEquals(bc, recoveredConvertable);
    }

    @Test
    void complexArray() {
        Message bm = new ListBsonMessage(new ListBsonConvertable());
        byte[] buffer = bm.serialize();
        byte[] arr = new byte[buffer.length - Integer.BYTES];
        System.arraycopy(buffer, 4, arr, 0, arr.length);

        BsonDecoder.registerBsonMessage(ExampleBsonConvertable.class, ExampleBsonMessage.class, 1005);
        BsonDecoder.registerBsonMessage(ListBsonConvertable.class, ListBsonMessage.class, 1006);

        ListBsonMessage msg = (ListBsonMessage) new BsonDecoder(null).decode(arr);
        msg.convertBack();
        assertTrue(ListBsonConvertable.called);
    }

    static class ExampleBsonConvertable implements BsonConvertable {
        private String stringData;
        private int intData;
        private double doubleData;
        private byte[] byteArrayData;

        public ExampleBsonConvertable() {

        }

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
        public void convertToBson(BSONObject obj) {
            obj.put("str", stringData);
            obj.put("int", intData);
            obj.put("double", doubleData);
            obj.put("byteArray", byteArrayData);
        }

        @Override
        public void fromBson(BsonObjectCompact obj) {
            stringData = obj.requireObject("str");
            intData = obj.requireObject("int");
            doubleData = obj.requireObject("double");
            byteArrayData = obj.requireObject("byteArray");
        }
    }

    static class ListBsonConvertable implements BsonConvertable {
        static boolean called = false;

        public ListBsonConvertable() {

        }

        @Override
        public void convertToBson(BSONObject bo) {
            ArrayList<ExampleBsonConvertable> list = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                list.add(new ExampleBsonConvertable("str", 5, 10.0, new byte[i]));
            }
            bo.put("data", 3);
            bo.put("list", Helper.toBsonList(list));
        }

        public void fromBson(BsonObjectCompact bo) {
            assertEquals(3, bo.<Integer>get("data"));
            List<ExampleBsonConvertable> list = BsonConvertable.Helper.fromBsonList(bo.get("list"), 1005);
            assertEquals(3, list.size());
            for (int i = 0; i < 3; ++i) {
                ExampleBsonConvertable convertable = list.get(i);
                assertEquals("str", convertable.stringData);
                assertEquals(5, convertable.intData);
                assertEquals(10.0, convertable.doubleData);
                assertEquals(i, convertable.byteArrayData.length);
            }
            called = true;
        }

    }

    static class ExampleBsonMessage extends BsonMessage<ExampleBsonConvertable> {
        public ExampleBsonMessage(ExampleBsonConvertable object) {
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

    static class ListBsonMessage extends BsonMessage<ListBsonConvertable> {

        public ListBsonMessage(ListBsonConvertable object) {
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
}
