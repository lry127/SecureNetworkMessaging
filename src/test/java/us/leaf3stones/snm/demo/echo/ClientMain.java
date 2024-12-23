package us.leaf3stones.snm.demo.echo;

import us.leaf3stones.snm.client.HttpSecClient;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.message.GeneralPayloadMessage;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        HttpSecClient client = HttpSecClient.connectToServer("localhost", 25100, new BaseMessageDecoder());
        GeneralPayloadMessage echoRequest = GeneralPayloadMessage.newInstance("echo", "hi, SecureNetworkMessaging!");
        client.sendMessage(echoRequest);
        GeneralPayloadMessage echoResponse = (GeneralPayloadMessage) client.readMessage();
        System.err.println("response: " + echoResponse.getPayloadAsString());
        client.shutdown();
    }
}
