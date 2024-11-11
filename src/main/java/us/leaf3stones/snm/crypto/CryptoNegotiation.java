package us.leaf3stones.snm.crypto;

import us.leaf3stones.snm.message.InputStreamUtil;
import us.leaf3stones.snm.common.NetIOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

/**
 * helper class to help communication entities establish a secure connection for subsequent messages
 */
public class CryptoNegotiation {
    private static boolean isKeyCacheEnabled = true;
    private static byte[] cachedServerPublicKey;
    private static byte[] cachedServerPrivateKey;

    private static void setEnableKeyCache(boolean isEnabled) {
        isKeyCacheEnabled = isEnabled;
    }

    public static NegotiatedCryptoNative negotiateAsClient(@SuppressWarnings("unused") InputStream in, OutputStream out, String serverPublicKey) throws IOException, GeneralSecurityException {
        byte[] serverPublicKeyBytes;
        if (isKeyCacheEnabled) {
            if (cachedServerPublicKey == null) {
                cachedServerPublicKey = NegotiatedCryptoNative.decodeKey(serverPublicKey, "server public key");
            }
            serverPublicKeyBytes = cachedServerPublicKey;
        } else {
            serverPublicKeyBytes = NegotiatedCryptoNative.decodeKey(serverPublicKey, "server public key");
        }

        NegotiatedCryptoNative clientCrypto = new NegotiatedCryptoNative(null, null, serverPublicKeyBytes, false);
        byte[] myPublicKeyBytes = clientCrypto.getMyKeyExchangePublicKey();
        out.write(myPublicKeyBytes);
        return clientCrypto;
    }

    public static NegotiatedCryptoNative negotiateAsServer(InputStream in, @SuppressWarnings("unused") OutputStream out, String serverPublicKey, String serverPrivateKey) throws NetIOException, GeneralSecurityException {
        byte[] serverPublicKeyBytes;
        byte[] serverPrivateKeyBytes;
        if (isKeyCacheEnabled) {
            if (cachedServerPublicKey == null) {
                cachedServerPublicKey = NegotiatedCryptoNative.decodeKey(serverPublicKey, "server public key");
            }
            if (cachedServerPrivateKey == null) {
                cachedServerPrivateKey = NegotiatedCryptoNative.decodeKey(serverPrivateKey, "server private key");
            }
            serverPublicKeyBytes = cachedServerPublicKey;
            serverPrivateKeyBytes = cachedServerPrivateKey;
        } else {
            serverPublicKeyBytes = NegotiatedCryptoNative.decodeKey(serverPublicKey, "server public key");
            serverPrivateKeyBytes = NegotiatedCryptoNative.decodeKey(serverPrivateKey, "server private key");
        }

        byte[] clientPublicKeyBytes = InputStreamUtil.readNBytes(in, NegotiatedCryptoNative.KEY_EXCHANGE_KEY_SIZE);
        return new NegotiatedCryptoNative(serverPublicKeyBytes, serverPrivateKeyBytes, clientPublicKeyBytes, true);
    }

}
