package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.auth.AuthenticationChain;
import us.leaf3stones.snm.auth.NonceAuthenticator;
import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.server.HttpSecServer;
import us.leaf3stones.snm.server.HttpSecServerBuilder;


public class ServerMain {
    public static void main(String[] args) throws Exception {
        HttpSecServerBuilder builder = new HttpSecServerBuilder();
        builder.setPort(5000);
        builder.setHandlerFactory(new HandlerFactory() {
            @Override
            public MessageHandler createRequestHandler(HttpSecPeer peer) {
                return new ArithmeticOperationHandler(peer);
            }
        });
        // though we didn't use any predefined message, we can still include the
        // base decoder for easier migration if we later changed our minds and use
        // a predefined message
        builder.setMessageDecoder(new ArithmeticMessageDecoder(new BaseMessageDecoder()));
        builder.setRateLimitingPolicy(new CalculatorRateLimiting());
        builder.setAuthChain(new AuthenticationChain(NonceAuthenticator.class));
        HttpSecServer server = builder.build();
        server.accept(true);
    }
}
