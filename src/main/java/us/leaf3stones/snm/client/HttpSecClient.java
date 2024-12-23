package us.leaf3stones.snm.client;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.message.MessageDecoder;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.concurrent.Executors;

public class HttpSecClient extends HttpSecPeer {
    public HttpSecClient(Socket peer, MessageDecoder decoder) throws IOException {
        super(peer, decoder, Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    }

    public static HttpSecClient connectToServer(String host, int port, MessageDecoder decoder, Certificate caCert,
                                                KeyStore myKeyStore, char[] myKsPassword) {
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(myKeyStore, myKsPassword);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);  // Initialize an empty truststore
            trustStore.setCertificateEntry("caCert", caCert);
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            return new HttpSecClient(socket, decoder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpSecClient connectToServer(String host, int port, MessageDecoder decoder) {
        try(InputStream keystoreInputStream = HttpSecClient.class.getResourceAsStream("/client.p12")) {
            final char[] testKeyStorePassword = "password".toCharArray();
            KeyStore clientKeystore = KeyStore.getInstance("PKCS12");
            clientKeystore.load(keystoreInputStream, testKeyStorePassword);

            Certificate[] certChain = clientKeystore.getCertificateChain("1");
            Certificate caCert = certChain[certChain.length - 1];

            return connectToServer(host, port, decoder, caCert, clientKeystore, testKeyStorePassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
