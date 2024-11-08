package us.leaf3stones.snm.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.auth.AuthenticationChain;
import us.leaf3stones.snm.auth.Authenticator;
import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.crypto.LengthMessageCrypto;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.MessageDecoder;
import us.leaf3stones.snm.rate.RateLimiting;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final HttpSecPeer client;

    private final HandlerFactory handlerFactory;
    private final AuthenticationChain authChain;

    public ClientHandler(Socket client, ExecutorService executor, HandlerFactory handlerFactory, AuthenticationChain authChain, MessageDecoder decoder) throws IOException {
        this.client = new HttpSecPeer(client, decoder, executor);
        this.handlerFactory = handlerFactory;
        this.authChain = authChain;
    }

    public static int ipv4ToInt(String ipv4Address) {
        String[] octets = ipv4Address.split("\\.");
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Integer.parseInt(octets[i]) & 0xFF) << ((3 - i) * 8);
        }
        return result;
    }

    @Override
    public void run() {
        try {
            // when a client connects, wait a couple of millis seconds to avoid flooding us
            int ip = ipv4ToInt(client.getPeerSocket().getInetAddress().getHostAddress());
            int sleepMillis = RateLimiting.getInstance().getWaitingTimeFor(ip);
            Thread.sleep(sleepMillis);

            client.tryToNegotiateCryptoInfo(LengthMessageCrypto.serverKeyExchangePublicKey,
                    LengthMessageCrypto.serverKeyExchangePrivateKey, true);

            authChain.authenticate(client);
            MessageHandler clientMessageHandler = handlerFactory.createRequestHandler(client);
            clientMessageHandler.takeOver();

        } catch (Exception e) {
            logger.warn("exception occurred: closing connection. {}", e.getMessage());
            if (e instanceof Authenticator.AuthenticationException || e instanceof RateLimiting.TooManyRequestException) {
                String remoteHost = "unknown";
                try {
                    remoteHost = client.getPeerSocket().getRemoteSocketAddress().toString().replace("/", "");
                } catch (Exception ignored) {

                }
                logger.error("SPAM DETECTED: from {} : error: {}", remoteHost, e.getMessage());
                try {
                    // set tcp RST
                    client.getPeerSocket().setSoLinger(true, 0);
                } catch (IOException ignored) {

                }
            }
            client.shutdown();
        }
    }
}
