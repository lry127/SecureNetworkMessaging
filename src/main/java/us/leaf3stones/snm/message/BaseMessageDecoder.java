package us.leaf3stones.snm.message;

import java.nio.ByteBuffer;
import java.util.Set;

public class BaseMessageDecoder extends MessageDecoder {
    public BaseMessageDecoder() {
        super(null);
    }

    @Override
    protected Message convert(int messageId, ByteBuffer messageBody) throws DecodeException {
        //noinspection EnhancedSwitchMigration
        switch (messageId) {
            case MessageTypeIdentifiers.TYPE_KEEP_ALIVE:
                return new KeepAliveMessage(messageBody);
            case MessageTypeIdentifiers.TYPE_GENERAL_PAYLOAD_MESSAGE:
                return new GeneralPayloadMessage(messageBody);
            case MessageTypeIdentifiers.TYPE_RESPONSE_MESSAGE:
                return new ResponseMessage(messageBody);
            case MessageTypeIdentifiers.TYPE_AUTHENTICATION_MESSAGE:
                return new AuthenticationMessage(messageBody);
            case MessageTypeIdentifiers.TYPE_POW_AUTHENTICATION_MESSAGE:
                return new POWAuthenticationMessage(messageBody);
            case MessageTypeIdentifiers.TYPE_AUTHENTICATION_RESPONSE_MESSAGE:
                return new AuthenticationResponseMessage(messageBody);
            case MessageTypeIdentifiers.TYPE_GENERAL_FAILURE_MESSAGE:
                return new GeneralFailureResponse(messageBody);
            default:
                throw new AssertionError("can't reach here");
        }
    }

    @Override
    protected Set<Integer> getConvertableMessageIds() {
        return Set.of(MessageTypeIdentifiers.TYPE_KEEP_ALIVE,
                MessageTypeIdentifiers.TYPE_GENERAL_PAYLOAD_MESSAGE,
                MessageTypeIdentifiers.TYPE_RESPONSE_MESSAGE,
                MessageTypeIdentifiers.TYPE_AUTHENTICATION_MESSAGE,
                MessageTypeIdentifiers.TYPE_POW_AUTHENTICATION_MESSAGE,
                MessageTypeIdentifiers.TYPE_AUTHENTICATION_RESPONSE_MESSAGE,
                MessageTypeIdentifiers.TYPE_GENERAL_FAILURE_MESSAGE);
    }

    public static class MessageTypeIdentifiers {
        public static final int TYPE_KEEP_ALIVE = 1;
        public static final int TYPE_GENERAL_PAYLOAD_MESSAGE = 2;
        public static final int TYPE_RESPONSE_MESSAGE = 3;
        public static final int TYPE_AUTHENTICATION_MESSAGE = 4;
        public static final int TYPE_POW_AUTHENTICATION_MESSAGE = 5;
        public static final int TYPE_AUTHENTICATION_RESPONSE_MESSAGE = 6;
        public static final int TYPE_GENERAL_FAILURE_MESSAGE = 7;
    }

}
