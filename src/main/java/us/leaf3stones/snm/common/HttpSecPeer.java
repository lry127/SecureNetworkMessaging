package us.leaf3stones.snm.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.crypto.CryptoNegotiation;
import us.leaf3stones.snm.crypto.NegotiatedCryptoNative;
import us.leaf3stones.snm.message.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class HttpSecPeer {
    private static final Logger logger = LoggerFactory.getLogger(HttpSecPeer.class);
    private final Socket peer;
    private final InputStream peerIn;
    private final OutputStream peerOut;
    private final MessageDecoder decoder;
    private final PriorityMessageQueue messageQueue;
    private MessageFactory messageFactory;

    public HttpSecPeer(Socket peer, MessageDecoder decoder, ExecutorService executor) throws IOException {
        this.peer = peer;
        this.decoder = decoder;
        peerIn = peer.getInputStream();
        peerOut = peer.getOutputStream();
        this.messageQueue = new PriorityMessageQueue(20, executor, new MessageSender());
    }

    public HttpSecPeer(String host, int port, MessageDecoder decoder, ExecutorService executor) throws IOException {
        this(new Socket(host, port), decoder, executor);
    }

    public Socket getPeerSocket() {
        return peer;
    }

    public void tryToNegotiateCryptoInfo(String serverPublicKey, String serverPrivateKey, boolean isServer) throws GeneralSecurityException, IOException {
        NegotiatedCryptoNative crypto;
        if (isServer) {
            crypto = CryptoNegotiation.negotiateAsServer(peerIn, peerOut, serverPublicKey, serverPrivateKey);
        } else {
            crypto = CryptoNegotiation.negotiateAsClient(peerIn, peerOut, serverPublicKey);
        }
        messageFactory = new MessageFactory(crypto, decoder);
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
