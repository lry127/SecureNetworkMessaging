package us.leaf3stones.snm.handler;

import us.leaf3stones.snm.common.HttpSecPeer;

public abstract class MessageHandler {
    protected final HttpSecPeer peer;

    public MessageHandler(HttpSecPeer peer) {
        this.peer = peer;
    }

    public abstract void takeOver() throws Exception;
}

