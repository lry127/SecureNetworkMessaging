package us.leaf3stones.snm.auth;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.common.ProofOfWork;
import us.leaf3stones.snm.message.AuthenticationResponseMessage;
import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.POWAuthenticationMessage;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

public class ProofOfWorkAuthenticator extends Authenticator {
    private static final short MIN_BYPASS_MILLIS = 10 * 1000; // 10 sec
    private static final short REQUIRED_MATCH_LENGTH = 4;

    private static final Random randomGenerator = new SecureRandom();

    private final long base;
    private final long authBeginMillis;

    public ProofOfWorkAuthenticator(HttpSecPeer peer) {
        super(peer);
        base = randomGenerator.nextLong();
        authBeginMillis = System.currentTimeMillis();
    }

    @Override
    public void authenticate() throws IOException, AuthenticationException {
        POWAuthenticationMessage authMsg = POWAuthenticationMessage.newInstance(REQUIRED_MATCH_LENGTH, MIN_BYPASS_MILLIS, base);
        peer.sendMessage(authMsg);
        Message responseMsg = peer.readMessage();
        if (!(responseMsg instanceof AuthenticationResponseMessage authResponseMsg)) {
            throw new SecurityException("failed to auth peer. sending unrecognized auth response");
        }
        processAuthResponse(authResponseMsg);
    }

    private void processAuthResponse(AuthenticationResponseMessage authResponseMsg) throws AuthenticationException {
        if (authResponseMsg.isBypass()) {
            if (System.currentTimeMillis() < authBeginMillis + MIN_BYPASS_MILLIS) {
                throw new AuthenticationException("too early to skip auth check");
            }
            return;
        }
        if (!ProofOfWork.checkWork(base, authResponseMsg.getResponseAsLong(), REQUIRED_MATCH_LENGTH)) {
            throw new AuthenticationException("incorrect auth. rejecting connection");
        }
    }
}
