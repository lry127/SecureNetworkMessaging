package us.leaf3stones.snm.handler;

import us.leaf3stones.snm.common.HttpSecPeer;

public abstract class HandlerFactory {
    public abstract MessageHandler createRequestHandler(HttpSecPeer peer);
}
