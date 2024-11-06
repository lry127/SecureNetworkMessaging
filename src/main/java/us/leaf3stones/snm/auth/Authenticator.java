package us.leaf3stones.snm.auth;

import us.leaf3stones.snm.common.HttpSecPeer;

import java.io.IOException;

public abstract class Authenticator {
    protected final HttpSecPeer peer;

    public Authenticator(HttpSecPeer peer) {
        this.peer = peer;
    }

    public abstract void authenticate() throws IOException, AuthenticationException;

    public static class AuthenticationException extends SecurityException {
        AuthenticationException(String message) {
            super(message);
        }
    }
}
