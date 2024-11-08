package us.leaf3stones.snm.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class NegotiatedCryptoNativeTest {
    @Test
    void keyNegotiationWithCorrectKey() throws Exception {
        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");

        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);
        assertArrayEquals(client.getMyKeyExchangePublicKey(), server.getPeerKeyExchangePublicKey());
        assertArrayEquals(server.getMyKeyExchangePublicKey(), client.getPeerKeyExchangePublicKey());

        byte[] message = "message".repeat(1000).getBytes(StandardCharsets.UTF_8);
        encryptionAndDecryptionTestMutual(message, client, server);

        assertEquals(0, NegotiatedCryptoNative.getTotalInstanceCount());
        assertEquals(0, LengthMessageCrypto.getTotalInstanceCount());
    }

    void encryptionAndDecryptionTestMutual(byte[] ciphertext, NegotiatedCryptoNative peer1, NegotiatedCryptoNative peer2) throws GeneralSecurityException {
        encryptionAndDecryptionTest(ciphertext, peer1, peer2);
        encryptionAndDecryptionTest(ciphertext, peer2, peer1);
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

    @Test
    void assertServerDoesNotInitWithBadKeyPair() throws Exception {
        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
        // bad private key, does not match public key
        final String serverKeyExchangePrivateKey = "/A2F/Ap5ULw2MxMiKriNLziIldEa+7VMzGh0wiXk/ug=";

        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");
        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);

        assertThrows(GeneralSecurityException.class, () -> {
            byte[] message = "message".repeat(1000).getBytes(StandardCharsets.UTF_8);
            encryptionAndDecryptionTest(message, client, server);
        });
        assertThrows(GeneralSecurityException.class, () -> {
            byte[] message = "message".repeat(1000).getBytes(StandardCharsets.UTF_8);
            encryptionAndDecryptionTest(message, server, client);
        });

        assertEquals(0, NegotiatedCryptoNative.getTotalInstanceCount());
        assertEquals(0, LengthMessageCrypto.getTotalInstanceCount());
    }

    @Test
    void largeData() throws Exception {
        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");

        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);
        assertArrayEquals(client.getMyKeyExchangePublicKey(), server.getPeerKeyExchangePublicKey());
        assertArrayEquals(server.getMyKeyExchangePublicKey(), client.getPeerKeyExchangePublicKey());

        for (int i = 1; i < 10000; ++i) {
            byte[] message = "message".repeat(i).getBytes(StandardCharsets.UTF_8);
            encryptionAndDecryptionTestMutual(message, client, server);
        }

//        assertEquals(2, NegotiatedCryptoNative.getTotalInstanceCount());
        assertEquals(0, NegotiatedCryptoNative.getTotalInstanceCount());
        assertEquals(0, LengthMessageCrypto.getTotalInstanceCount());
    }

    @Test
    void extremelyLengthyCommunicationThatResultsInMessageIdFlip() throws Exception {
        if (true) {
            // disabled by default.
            return;
        }
        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");

        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);
        assertArrayEquals(client.getMyKeyExchangePublicKey(), server.getPeerKeyExchangePublicKey());
        assertArrayEquals(server.getMyKeyExchangePublicKey(), client.getPeerKeyExchangePublicKey());

        byte[] message = new byte[16];
        for (long i = 0; i < Integer.MAX_VALUE * 3L; ++i) {
            encryptionAndDecryptionTestMutual(message, client, server);
        }
    }


    @Test
    void concurrentAccessTest_Part1_MultiplyCommunicationPairs() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 100_000; ++i) {
            int idx = i;
            executor.execute(() -> {
                try {
                    if (idx % 10 != 0) {
                        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
                        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

                        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
                        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");

                        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
                        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);
                        assertArrayEquals(client.getMyKeyExchangePublicKey(), server.getPeerKeyExchangePublicKey());
                        assertArrayEquals(server.getMyKeyExchangePublicKey(), client.getPeerKeyExchangePublicKey());
                        byte[] message = "message".repeat(3).getBytes(StandardCharsets.UTF_8);
                        encryptionAndDecryptionTestMutual(message, client, server);

                    } else {
                        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
                        // bad private key, does not match public key
                        final String serverKeyExchangePrivateKey = "/A2F/Ap5ULw2MxMiKriNLziIldEa+7VMzGh0wiXk/ug=";

                        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
                        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
                        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");
                        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);

                        assertThrows(GeneralSecurityException.class, () -> {
                            byte[] message = "message".repeat(5).getBytes(StandardCharsets.UTF_8);
                            encryptionAndDecryptionTest(message, client, server);
                        });
                        assertThrows(GeneralSecurityException.class, () -> {
                            byte[] message = "message".repeat(5).getBytes(StandardCharsets.UTF_8);
                            encryptionAndDecryptionTest(message, server, client);
                        });

                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.close();
        assertEquals(0, LengthMessageCrypto.getTotalInstanceCount());
        assertEquals(0, NegotiatedCryptoNative.getTotalInstanceCount());
    }

    @Test
    void concurrentAccessTest_Part2_ParallelCommunicationPairs() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 100_000; ++i) {
            Random r = new Random();
            int idx = i;
            executor.execute(() -> {
                try {
                    if (idx % 10 != 0) {
                        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
                        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

                        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
                        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");

                        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
                        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);

                        byte[] message = new byte[16];
                        r.nextBytes(message);
                        try {
                            encryptionAndDecryptionTestMutual(message, client, server);
                        } catch (GeneralSecurityException e) {
                            fail(e);
                        }

                    } else {
                        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
                        // bad private key, does not match public key
                        final String serverKeyExchangePrivateKey = "/A2F/Ap5ULw2MxMiKriNLziIldEa+7VMzGh0wiXk/ug=";

                        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
                        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
                        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");
                        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);

                        for (int j = 0; j < 10; ++j) {
                            {
                                byte[] message = new byte[16];
                                r.nextBytes(message);
                                assertThrows(GeneralSecurityException.class, () -> {
                                    encryptionAndDecryptionTest(message, client, server);
                                });
                                assertThrows(GeneralSecurityException.class, () -> {
                                    encryptionAndDecryptionTest(message, server, client);
                                });
                            }
                        }

                    }
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
        executor.close();

        assertEquals(0, LengthMessageCrypto.getTotalInstanceCount());
        assertEquals(0, NegotiatedCryptoNative.getTotalInstanceCount());
    }

    @Test
    void benchmark() {
        long begin = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 1_000_000; ++i) {
            Random r = new Random();
            int idx = i;
            executor.execute(() -> {
                try {
                    if (idx % 10 != 0) {
                        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
                        final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

                        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
                        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");

                        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
                        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);


                        for (int j = 0; j < 3; ++j) {
                            byte[] message = new byte[16];
                            r.nextBytes(message);
                            try {
                                encryptionAndDecryptionTestMutual(message, client, server);
                            } catch (GeneralSecurityException e) {
                                fail(e);
                            }
                        }

                    } else {
                        final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
                        // bad private key, does not match public key
                        final String serverKeyExchangePrivateKey = "/A2F/Ap5ULw2MxMiKriNLziIldEa+7VMzGh0wiXk/ug=";

                        NegotiatedCryptoNative client = new NegotiatedCryptoNative(serverKeyExchangePublicKey);
                        final byte[] serverPubBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePublicKey, "server public key");
                        final byte[] serverPriBytes = NegotiatedCryptoNative.decodeKey(serverKeyExchangePrivateKey, "server private key");
                        NegotiatedCryptoNative server = new NegotiatedCryptoNative(serverPubBytes, serverPriBytes, client.getMyKeyExchangePublicKey(), true);

                        for (int j = 0; j < 3; ++j) {
                            byte[] message = new byte[16];
                            r.nextBytes(message);
                            assertThrows(GeneralSecurityException.class, () -> {
                                encryptionAndDecryptionTest(message, client, server);
                            });
                            assertThrows(GeneralSecurityException.class, () -> {
                                encryptionAndDecryptionTest(message, server, client);
                            });
                        }
                    }
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
        executor.close();

        long duration = System.currentTimeMillis() - begin;
        System.err.printf("took %.3f sec to finish\n", duration / 1000.0);
    }

}
