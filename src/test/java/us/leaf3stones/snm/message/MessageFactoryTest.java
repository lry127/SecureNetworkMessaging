package us.leaf3stones.snm.message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class MessageFactoryTest {
    @Test
    public void encodeAndDecodeTest() throws Exception {
        GeneralPayloadMessage payload = GeneralPayloadMessage.newInstance("name", "payload");
        MessageFactory factory = new MessageFactory(new BaseMessageDecoder());
        ByteArrayInputStream in = new ByteArrayInputStream(factory.serializeMessage(payload));
        Message read = factory.parseMessage(in);
        Assertions.assertInstanceOf(GeneralPayloadMessage.class, read);
    }
}
