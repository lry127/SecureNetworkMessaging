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
        // we used BaseMessageDecoder internally, if you have your own decoder, chain it as parent
        builder.setMessageDecoder(new ArithmeticMessageDecoder(new BaseMessageDecoder()));
        builder.setRateLimitingPolicy(new CalculatorRateLimiting());
        // fight against replay attack by using a nonce
        builder.setAuthChain(new AuthenticationChain(NonceAuthenticator.class));
        HttpSecServer server = builder.build();
        server.accept(true);
    }
}
