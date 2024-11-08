package us.leaf3stones.snm.client;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.crypto.LengthMessageCrypto;
import us.leaf3stones.snm.message.MessageDecoder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

public class HttpSecClient extends HttpSecPeer {
    public HttpSecClient(String host, int port, MessageDecoder decoder) throws IOException {
        super(host, port, decoder, Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory()));
        try {
            tryToNegotiateCryptoInfo(LengthMessageCrypto.serverKeyExchangePublicKey, null, false);
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}
