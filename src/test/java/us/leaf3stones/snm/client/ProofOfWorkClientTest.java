package us.leaf3stones.snm.client;

import org.junit.jupiter.api.Test;
import us.leaf3stones.snm.auth.AuthenticationChain;
import us.leaf3stones.snm.auth.ProofOfWorkAuthenticator;
import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.demo.echo.EchoMessageHandler;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.message.GeneralPayloadMessage;
import us.leaf3stones.snm.server.HttpSecServer;
import us.leaf3stones.snm.server.HttpSecServerBuilder;

import static org.junit.jupiter.api.Assertions.*;

class ProofOfWorkClientTest {

    @Test
    void ensureTimeLimitIsHonored() {
        short computeAtMost = 300;
        Thread bomb = new Thread(() -> {
            try {
                Thread.sleep(computeAtMost * 2);
            } catch (InterruptedException e) {
                return;
            }
            fail("timer expired");
        });
        bomb.start();
        Long res = new ProofOfWorkClient(null).computeNonceBlockingAtMost(computeAtMost, (short) 0, (short) 20);
        assertNull(res);
        bomb.interrupt();
    }

    @Test
    void proofOfWorkInActionTest() throws Exception {
        GeneralPayloadMessage.newInstance("hi", new byte[0]);
        HttpSecServerBuilder builder = new HttpSecServerBuilder();
        builder.setPort(5008);
        builder.setHandlerFactory(new HandlerFactory() {
            @Override
            public MessageHandler createRequestHandler(HttpSecPeer peer) {
                return new EchoMessageHandler(peer);
            }
        });
        builder.setMessageDecoder(new BaseMessageDecoder());
        builder.setAuthChain(new AuthenticationChain(ProofOfWorkAuthenticator.class));
        HttpSecServer server = builder.build();
        server.accept(false);

        // run the client
        HttpSecClient client = HttpSecClient.connectToServer("localhost", 5008, new BaseMessageDecoder());
        new ProofOfWorkClient(client).authenticateToServer();
        GeneralPayloadMessage echoRequest = GeneralPayloadMessage.newInstance("echo", "hi, SecureNetworkMessaging!");
        client.sendMessage(echoRequest);
        GeneralPayloadMessage echoResponse = (GeneralPayloadMessage) client.readMessage();
        assertEquals(echoRequest.getPayloadAsString(), echoResponse.getPayloadAsString());
    }
}