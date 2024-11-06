package us.leaf3stones.snm.demo.echo;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.server.HttpSecServer;
import us.leaf3stones.snm.server.HttpSecServerBuilder;

import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        HttpSecServerBuilder builder = new HttpSecServerBuilder();
        builder.setPort(5000);
        builder.setHandlerFactory(new HandlerFactory() {
            @Override
            public MessageHandler createRequestHandler(HttpSecPeer peer) {
                return new EchoMessageHandler(peer);
            }
        });
        builder.setMessageDecoder(new BaseMessageDecoder());
        HttpSecServer server = builder.build();
        server.accept(true);
    }
}
