package us.leaf3stones.snm.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.auth.AuthenticationChain;
import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.message.MessageDecoder;
import us.leaf3stones.snm.rate.RateLimiting;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpSecServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpSecServer.class);

    private final int port;
    private final HandlerFactory handlerFactory;
    private final AuthenticationChain authChain;
    private final ExecutorService executor;
    private final MessageDecoder decoder;
    private final SSLContext sslContext;
    private SSLServerSocket serverSocket;

    public HttpSecServer(int port, HandlerFactory handlerFactory, RateLimiting.RateLimitingPolicy rateLimitingPolicy,
                         AuthenticationChain authChain, MessageDecoder decoder, SSLContext sslContext) {
        this.port = port;
        this.handlerFactory = handlerFactory;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        RateLimiting.init(executor, rateLimitingPolicy);
        this.authChain = authChain;
        this.decoder = decoder;
        this.sslContext = sslContext;
    }

    public void accept(boolean shouldBlock) throws IOException {
        serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port, 0, Inet4Address.getLoopbackAddress());
        serverSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});

        Runnable acceptWork = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    logger.info("accept client from {}", clientSocket.getRemoteSocketAddress().toString().replace('/', ' '));
                    executor.execute(new ClientHandler(clientSocket, executor, handlerFactory, authChain, decoder));
                } catch (Exception e) {
                    logger.warn("failed to accept {}", e.getMessage());
                }
            }
        };
        if (shouldBlock) {
            acceptWork.run();
        } else {
            executor.execute(acceptWork);
        }
    }

    public static void main(String[] args) throws Exception {
        Integer port = null;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {

            }
        }
        if (port == null) {
            port = 25003;
        }

        HttpSecServerBuilder builder = new HttpSecServerBuilder();

        if (args.length > 2) {
            try (InputStream keyStoreIn = new FileInputStream(args[1])) {
                builder.setKeyStoreStream(keyStoreIn, args[2].toCharArray());
            }
        }

        builder.setPort(port).setHandlerFactory(new HandlerFactory() {
                    @Override
                    public MessageHandler createRequestHandler(HttpSecPeer peer) {
                        return null;
                    }
                })
                .setMessageDecoder(new BaseMessageDecoder()).build().accept(true);
    }

}
