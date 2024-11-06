package us.leaf3stones.snm.demo.echo;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.GeneralPayloadMessage;
import us.leaf3stones.snm.message.Message;

public class EchoMessageHandler extends MessageHandler {
    public EchoMessageHandler(HttpSecPeer peer) {
        super(peer);
    }

    @Override
    public void takeOver() throws Exception {
        Message received = peer.readMessage(); // read a message from client
        if (!(received instanceof GeneralPayloadMessage payloadMsg)) {
            throw new RuntimeException("client sent unexpected type of message");
        }
        if (!"echo".equals(payloadMsg.getName())) {
            throw new RuntimeException("we expect echo message from client");
        }
        String message = payloadMsg.getPayloadAsString(); // client message
        Message response = GeneralPayloadMessage.newInstance("echo_response", message); // preparing response
        peer.sendMessage(response); // send it back to client
        peer.shutdown();
    }
}
