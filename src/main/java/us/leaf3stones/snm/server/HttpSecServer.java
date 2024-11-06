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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpSecServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpSecServer.class);

    private final int port;
    private final HandlerFactory handlerFactory;
    private final AuthenticationChain authChain;
    private final ExecutorService executor;
    private final MessageDecoder decoder;
    private ServerSocket serverSocket;

    public HttpSecServer(int port, HandlerFactory handlerFactory, RateLimiting.RateLimitingPolicy rateLimitingPolicy, AuthenticationChain authChain, MessageDecoder decoder) {
        this.port = port;
        this.handlerFactory = handlerFactory;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        RateLimiting.init(executor, rateLimitingPolicy);
        this.authChain = authChain;
        this.decoder = decoder;
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
            port = 5000;
        }

        new HttpSecServerBuilder().setPort(port).setHandlerFactory(new HandlerFactory() {
            @Override
            public MessageHandler createRequestHandler(HttpSecPeer peer) {
                return null;
            }
        }).setMessageDecoder(new BaseMessageDecoder()).build().accept(true);
    }

    public void accept(boolean shouldBlock) throws IOException {
        serverSocket = new ServerSocket(port);
        Runnable acceptWork = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("accept client from {}", clientSocket.getRemoteSocketAddress().toString().replace('/', ' '));
                    executor.execute(new ClientHandler(clientSocket, handlerFactory, authChain, decoder));
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
}
