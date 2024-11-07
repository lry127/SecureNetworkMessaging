package us.leaf3stones.snm.common;

import us.leaf3stones.snm.crypto.CryptoNegotiation;
import us.leaf3stones.snm.crypto.NegotiatedCryptoNative;
import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.MessageDecoder;
import us.leaf3stones.snm.message.MessageFactory;
import us.leaf3stones.snm.message.NetIOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class HttpSecPeer {
    private final Socket peer;
    private final InputStream peerIn;
    private final OutputStream peerOut;
    private final MessageDecoder decoder;
    private MessageFactory messageFactory;

    public HttpSecPeer(Socket peer, MessageDecoder decoder) throws IOException {
        this.peer = peer;
        this.decoder = decoder;
        peerIn = peer.getInputStream();
        peerOut = peer.getOutputStream();
    }

    public HttpSecPeer(String host, int port, MessageDecoder decoder) throws IOException {
        this(new Socket(host, port), decoder);
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
        return messageFactory.parseMessage(peerIn);
    }

    public void sendMessage(Message message) throws NetIOException {
        try {
            peerOut.write(messageFactory.serializeMessage(message));
        } catch (IOException e) {
            throw new NetIOException(e, true);
        }
    }

    public void shutdown() {
        try {
            peer.close();
        } catch (Exception ignored) {

        }
    }
}
