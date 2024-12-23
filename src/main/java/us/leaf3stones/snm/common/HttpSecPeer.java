package us.leaf3stones.snm.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.message.KeepAliveMessage;
import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.MessageDecoder;
import us.leaf3stones.snm.message.MessageFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class HttpSecPeer {
    private static final Logger logger = LoggerFactory.getLogger(HttpSecPeer.class);
    private final Socket peer;
    private final InputStream peerIn;
    private final OutputStream peerOut;
    private final PriorityMessageQueue messageQueue;
    private final MessageFactory messageFactory;

    public HttpSecPeer(Socket peer, MessageDecoder decoder, ExecutorService executor) throws IOException {
        this.peer = peer;
        peerIn = peer.getInputStream();
        peerOut = peer.getOutputStream();
        this.messageQueue = new PriorityMessageQueue(20, executor, new MessageSender());
        messageFactory = new MessageFactory(decoder);
    }

    public Socket getPeerSocket() {
        return peer;
    }


    public Message readMessage() throws NetIOException {
        Message received = messageFactory.parseMessage(peerIn);
        if (received instanceof KeepAliveMessage) {
            return readMessage();
        }
        return received;
    }

    public void sendMessageWithHighPriority(Message message) throws NetIOException {
        messageQueue.sendMessageWithHighPriority(message);
    }

    public void sendMessageWithRealtimePriority(Message message) throws NetIOException {
        messageQueue.sendMessageWithRealtimePriority(message);
    }

    public void sendMessageWithNormalPriority(Message message) throws NetIOException {
        messageQueue.sendMessageWithNormalPriority(message);
    }

    public void sendMessage(Message message) throws NetIOException {
        messageQueue.sendMessageWithNormalPriority(message);
    }

    public void enableKeepAlive(int keepAliveInterval) throws NetIOException {
        messageQueue.enableKeepAlive(keepAliveInterval);
    }

    public void shutdown() {
        try {
            peer.close();
            messageQueue.abort();
        } catch (Exception ignored) {

        }
    }

    private class MessageSender implements Function<Message, Void> {
        @Override
        public Void apply(Message message) {
            try {
                peerOut.write(messageFactory.serializeMessage(message));
                return null;
            } catch (IOException e) {
                logger.error("failed to send message to peer. error: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
