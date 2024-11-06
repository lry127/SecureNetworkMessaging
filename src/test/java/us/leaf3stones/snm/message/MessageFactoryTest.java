package us.leaf3stones.snm.message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import us.leaf3stones.snm.crypto.CryptoNegotiation;
import us.leaf3stones.snm.crypto.NegotiatedCryptoNative;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MessageFactoryTest {

    @Test
    void testParse() throws Exception {
        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[128]);
        ByteArrayOutputStream out = new ByteArrayOutputStream(128);

        NegotiatedCryptoNative client = CryptoNegotiation.negotiateAsClient(in, out, serverKeyExchangePublicKey);
        byte[] clientWrote = new byte[out.size()];
        System.arraycopy(out.toByteArray(), 0, clientWrote, 0, clientWrote.length);
        in = new ByteArrayInputStream(clientWrote);
        out = new ByteArrayOutputStream(128);
        NegotiatedCryptoNative server = CryptoNegotiation.negotiateAsServer(in, out, serverKeyExchangePublicKey, serverKeyExchangePrivateKey);

        MessageFactory clientFactory = new MessageFactory(client, new BaseMessageDecoder());
        MessageFactory serverFactory = new MessageFactory(server, new BaseMessageDecoder());

        Message msg = GeneralPayloadMessage.newInstance("test", "data");
        byte[] enc = clientFactory.serializeMessage(msg);
        System.err.println(Arrays.toString(enc));

        InputStream in2 = new ByteArrayInputStream(enc);
        Message recMsg = serverFactory.parseMessage(in2);
        Assertions.assertInstanceOf(GeneralPayloadMessage.class, recMsg);
        GeneralPayloadMessage payloadMessage = (GeneralPayloadMessage) recMsg;
        Assertions.assertEquals("test", payloadMessage.getName());
        Assertions.assertEquals("data", payloadMessage.getPayloadAsString());


    }

    @Test
    void benchmark() throws Exception {
        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[128]);
        ByteArrayOutputStream out = new ByteArrayOutputStream(128);

        NegotiatedCryptoNative client = CryptoNegotiation.negotiateAsClient(in, out, serverKeyExchangePublicKey);
        byte[] clientWrote = new byte[out.size()];
        System.arraycopy(out.toByteArray(), 0, clientWrote, 0, clientWrote.length);
        in = new ByteArrayInputStream(clientWrote);
        out = new ByteArrayOutputStream(128);
        NegotiatedCryptoNative server = CryptoNegotiation.negotiateAsServer(in, out, serverKeyExchangePublicKey, serverKeyExchangePrivateKey);

        MessageFactory clientFactory = new MessageFactory(client, new BaseMessageDecoder());
        MessageFactory serverFactory = new MessageFactory(server, new BaseMessageDecoder());

        ExecutorService executor = Executors.newFixedThreadPool(20);

        long testBegin = System.currentTimeMillis();
        for (int i = 0; i < 3_000_000; ++i) {
            executor.execute(() -> {
                byte[] data = new byte[256];
                Message msg = GeneralPayloadMessage.newInstance("test", data);
                byte[] enc = clientFactory.serializeMessage(msg);
                InputStream encryptedDateInput = new ByteArrayInputStream(enc);
                Message recMsg = null;
                try {
                    recMsg = serverFactory.parseMessage(encryptedDateInput);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                GeneralPayloadMessage payloadMessage = (GeneralPayloadMessage) recMsg;
                Assertions.assertEquals("test", payloadMessage.getName());
                Assertions.assertArrayEquals(data, payloadMessage.getPayload());
            });

        }
        executor.close();
        long testEnd = System.currentTimeMillis();
        System.err.printf("it took %.3f seconds to finish.", (testEnd - testBegin) / 1000.0);


    }
}