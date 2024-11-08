package us.leaf3stones.snm.auth;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.message.AuthenticationMessage;
import us.leaf3stones.snm.message.AuthenticationResponseMessage;
import us.leaf3stones.snm.message.Message;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

public class NonceAuthenticator extends Authenticator {
    private static final Random randomGenerator = new SecureRandom();
    private final long base;

    public NonceAuthenticator(HttpSecPeer peer) {
        super(peer);
        base = randomGenerator.nextLong();
    }

    @Override
    public void authenticate() throws IOException, AuthenticationException {
        AuthenticationMessage authMsg = AuthenticationMessage.newInstance(base);
        peer.sendMessage(authMsg);
        Message responseMsg = peer.readMessage();
        if (!(responseMsg instanceof AuthenticationResponseMessage authResponseMsg)) {
            throw new SecurityException("failed to auth peer. sending unrecognized auth response");
        }
        long expected = base + 1;
        long actual = authResponseMsg.getResponseAsLong();
        if (actual != expected) {
            throw new AuthenticationException("incorrect auth. rejecting connection. expected response: " + expected + " actual: " + actual);
        }
    }
}
