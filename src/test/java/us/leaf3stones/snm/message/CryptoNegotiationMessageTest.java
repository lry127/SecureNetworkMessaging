package us.leaf3stones.snm.message;

import org.junit.jupiter.api.Test;
import us.leaf3stones.snm.crypto.CryptoNegotiation;
import us.leaf3stones.snm.crypto.LengthMessageCrypto;
import us.leaf3stones.snm.crypto.NegotiatedCryptoNative;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoNegotiationMessageTest {
    @Test
    void successfulKeyExchange() throws Exception {
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

        Random r = new Random();
        for (int i = 0; i < 10; ++i) {
            byte[] testData = new byte[r.nextInt(1024, 10240)];
            r.nextBytes(testData);
            encryptionAndDecryptionTestMutual(testData, client, server);
        }


    }

    void encryptionAndDecryptionTestMutual(byte[] message, NegotiatedCryptoNative peer1, NegotiatedCryptoNative peer2) throws GeneralSecurityException {
        encryptionAndDecryptionTest(message, peer1, peer2);
        encryptionAndDecryptionTest(message, peer2, peer1);
    }

    void encryptionAndDecryptionTest(byte[] cleartext, NegotiatedCryptoNative sender, NegotiatedCryptoNative receiver) throws GeneralSecurityException {
        LengthMessageCrypto sendOut = sender.createNewLengthMessageForEncryption();
        byte[] encrypted = sendOut.encrypt(cleartext);
        assertEquals(LengthMessageCrypto.getFullEncryptionOverhead() + cleartext.length, encrypted.length);

        byte[] header = new byte[LengthMessageCrypto.getHeaderLength()];
        System.arraycopy(encrypted, 0, header, 0, header.length);

        byte[] body = new byte[encrypted.length - header.length];
        System.arraycopy(encrypted, header.length, body, 0, body.length);

        LengthMessageCrypto received = receiver.createNewLengthMessageForDecryption(header);
        assertEquals(body.length, received.getEncryptedBodySize());
        byte[] recoveredCleartext = received.decrypt(body);
        assertArrayEquals(cleartext, recoveredCleartext);
    }
}