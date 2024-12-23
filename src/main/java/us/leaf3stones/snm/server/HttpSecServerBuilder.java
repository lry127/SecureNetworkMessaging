package us.leaf3stones.snm.server;

import us.leaf3stones.snm.auth.AuthenticationChain;
import us.leaf3stones.snm.handler.HandlerFactory;
import us.leaf3stones.snm.message.MessageDecoder;
import us.leaf3stones.snm.rate.RateLimiting;
import us.leaf3stones.snm.rate.UnlimitedRateLimitingPolicy;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class HttpSecServerBuilder {
    private Integer port;
    private HandlerFactory handlerFactory;
    private RateLimiting.RateLimitingPolicy rateLimitingPolicy;
    private AuthenticationChain authChain;
    private MessageDecoder decoder;
    private SSLContext sslContext;

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

    public HttpSecServerBuilder setServerKey(Certificate caCert,
                                             KeyStore myKeyStore, char[] myKsPassword) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(myKeyStore, myKsPassword);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("caCert", caCert);
            tmf.init(trustStore);

            sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);
            this.sslContext = sslContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public HttpSecServerBuilder setServerKeyStoreStream(InputStream keyStoreStream, char[] keystorePass) throws Exception {
        KeyStore serverKs = KeyStore.getInstance("PKCS12");
        serverKs.load(keyStoreStream, keystorePass);

        Certificate[] certChain = serverKs.getCertificateChain("1");
        Certificate caCert = certChain[certChain.length - 1];

        setServerKey(caCert, serverKs, "password".toCharArray());
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
        if (sslContext == null) {
            try (InputStream debugKsIn = HttpSecServerBuilder.class.getResourceAsStream("/server.p12")) {
                final char[] testKeyStorePassword = "password".toCharArray();
                setServerKeyStoreStream(debugKsIn, testKeyStorePassword);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new HttpSecServer(port, handlerFactory, rateLimitingPolicy, authChain, decoder, sslContext);
    }
}