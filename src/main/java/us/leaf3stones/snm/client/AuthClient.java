package us.leaf3stones.snm.client;

import us.leaf3stones.snm.common.HttpSecPeer;

public abstract class AuthClient {
    protected final HttpSecPeer client;

    public AuthClient(HttpSecPeer client) {
        this.client = client;
    }

    public abstract void authenticateToServer() throws Exception;
}
