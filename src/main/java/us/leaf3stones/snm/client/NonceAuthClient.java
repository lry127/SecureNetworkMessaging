package us.leaf3stones.snm.client;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.message.AuthenticationMessage;
import us.leaf3stones.snm.message.AuthenticationResponseMessage;
import us.leaf3stones.snm.message.Message;

import java.io.IOException;

public class NonceAuthClient extends AuthClient {
    public NonceAuthClient(HttpSecPeer client) {
        super(client);
    }

    public void authenticateToServer() throws IOException, SecurityException {
        Message authRequestMessage = client.readMessage();
        if (!(authRequestMessage instanceof AuthenticationMessage)) {
            throw new IllegalStateException("wtf? server don't require auth and we consumed an message?!");
        }
        //noinspection PatternVariableCanBeUsed
        AuthenticationMessage authMsg = (AuthenticationMessage) authRequestMessage;

        long response = authMsg.getBase() + 1;
        AuthenticationResponseMessage responseMsg;
        responseMsg = AuthenticationResponseMessage.newInstance(false, response);
        client.sendMessage(responseMsg);
    }

}
