package us.leaf3stones.snm.server;

import us.leaf3stones.snm.auth.AuthenticationChain;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.message.MessageDecoder;
import us.leaf3stones.snm.rate.RateLimiting;
import us.leaf3stones.snm.rate.UnlimitedRateLimitingPolicy;

public class HttpSecServerBuilder {
    private Integer port;
    private HandlerFactory handlerFactory;
    private RateLimiting.RateLimitingPolicy rateLimitingPolicy;
    private AuthenticationChain authChain;
    private MessageDecoder decoder;

    public HttpSecServerBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public HttpSecServerBuilder setHandlerFactory(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
        return this;
    }

    public HttpSecServerBuilder setRateLimitingPolicy(RateLimiting.RateLimitingPolicy rateLimitingPolicy) {
        this.rateLimitingPolicy = rateLimitingPolicy;
        return this;
    }

    public HttpSecServerBuilder setAuthChain(AuthenticationChain authChain) {
        this.authChain = authChain;
        return this;
    }

    public HttpSecServerBuilder setMessageDecoder(MessageDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    public HttpSecServer build() {
        if (rateLimitingPolicy == null) {
            rateLimitingPolicy = new UnlimitedRateLimitingPolicy();
        }
        if (authChain == null) {
            try {
                authChain = new AuthenticationChain();
            } catch (NoSuchMethodException e) {
                throw new AssertionError("can't throw " + e);
            }
        }
        if (port == null) {
            throw new IllegalStateException("port can't be null");
        }
        if (handlerFactory == null) {
            throw new IllegalStateException("handler factory can't be null");
        }
        if (decoder == null) {
            throw new IllegalStateException("message decoder can't be null");
        }

        return new HttpSecServer(port, handlerFactory, rateLimitingPolicy, authChain, decoder);
    }
}