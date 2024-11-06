package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class GeneralPayloadMessage extends Message {
    private byte[] name;
    private byte[] payload;

    public GeneralPayloadMessage(ByteBuffer buffer) {
        super(buffer);
    }

    private GeneralPayloadMessage(byte[] name, byte[] payload) {
        super();
        this.name = name;
        this.payload = payload;
    }

    public static GeneralPayloadMessage newInstance(String name, String payload) {
        return new GeneralPayloadMessage(name.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));
    }

    public static GeneralPayloadMessage newInstance(String name, byte[] payload) {
        return new GeneralPayloadMessage(name.getBytes(StandardCharsets.UTF_8), payload);
    }

    @Override
    int getTypeIdentifier() {
        return BaseMessageDecoder.MessageTypeIdentifiers.TYPE_GENERAL_PAYLOAD_MESSAGE;
    }

    public String getName() {
        return new String(name, StandardCharsets.UTF_8);
    }

    public String getPayloadAsString() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    protected int peekDataSize() {
        return lengthWithHeader(name) + lengthWithHeader(payload);
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        sizedPut(name, buf);
        sizedPut(payload, buf);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        name = sizedRead(buf);
        payload = sizedRead(buf);
    }
}
