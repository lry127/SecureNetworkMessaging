package us.leaf3stones.snm.crypto;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Base64;


public class NegotiatedCryptoNative {
    public static final int KEY_EXCHANGE_KEY_SIZE = 32;
    private static final int SYMMETRIC_CRYPTO_KEY_SIZE = 32;

    static {
        CryptoInitializer.initNativeCrypto();
    }

    private final byte[] transmitKey = new byte[SYMMETRIC_CRYPTO_KEY_SIZE];
    private final byte[] receiveKey = new byte[SYMMETRIC_CRYPTO_KEY_SIZE];
    private final byte[] myKeyExchangePublicKey = new byte[KEY_EXCHANGE_KEY_SIZE];
    private final byte[] myKeyExchangePrivateKey = new byte[KEY_EXCHANGE_KEY_SIZE];
    private final byte[] peerKeyExchangePublicKey;


    public NegotiatedCryptoNative(byte[] myPublicKey, byte[] myPrivateKey, byte[] peerPublicKey, boolean isServer) throws GeneralSecurityException {
        validateKey(myPublicKey, "my public key", true);
        validateKey(myPrivateKey, "my private key", true);
        validateKey(peerPublicKey, "peer public key", false);
        if ((myPublicKey == null && myPrivateKey != null) || (myPublicKey != null && myPrivateKey == null)) {
            throw new IllegalArgumentException("my public key and private key must both be set or unset");
        }
        long nativeHandle = nativeInit(myPublicKey, myPrivateKey, peerPublicKey, isServer);
        if (nativeHandle == 0) {
            throw new GeneralSecurityException("failed to init crypto object with given keys");
        }

        NativeBuffer keyBuffer = new NativeBuffer(SYMMETRIC_CRYPTO_KEY_SIZE * 2 + KEY_EXCHANGE_KEY_SIZE * 2);
        fillBufferWithKeys(nativeHandle, keyBuffer.getHandle());
        ByteBuffer readBuffer = keyBuffer.wrapAsByteBuffer();
        readBuffer.get(transmitKey);
        readBuffer.get(receiveKey);
        readBuffer.get(myKeyExchangePublicKey);
        readBuffer.get(myKeyExchangePrivateKey);
        keyBuffer.clean();
        peerKeyExchangePublicKey = peerPublicKey;
    }

    public NegotiatedCryptoNative(String serverPublicKey) throws GeneralSecurityException {
        this(null, null, decodeKey(serverPublicKey, "server public key"), false);
    }

    static native int initSodiumLibrary();

    static native int getTotalInstanceCount();

    public static byte[] decodeKey(String key, String keyName) {
        if (key == null) {
            throw new IllegalArgumentException(keyName + " can't be null");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException("decode " + keyName + " failed. Content is invalid base64 data: " + key);
        }
        return decoded;
    }

    private static void validateKey(byte[] key, String keyName, boolean nullable) {
        if (key == null) {
            if (nullable) {
                return;
            }
            throw new IllegalArgumentException(keyName + " can't be null");
        }

        if (key.length != NegotiatedCryptoNative.KEY_EXCHANGE_KEY_SIZE) {
            throw new IllegalArgumentException("expected " + keyName + " to have " + NegotiatedCryptoNative.KEY_EXCHANGE_KEY_SIZE + " bytes. But got " + key.length + " bytes.");
        }
    }

    private static native void fillBufferWithKeys(long cryptoPtr, long bufferPtr);

    private native long nativeInit(byte[] myPublicKey, byte[] myPrivateKey, byte[] peerPublicKey, boolean isServer);

    public byte[] getMyKeyExchangePublicKey() {
        return myKeyExchangePublicKey;
    }

    public byte[] getMyKeyExchangePrivateKey() {
        return myKeyExchangePrivateKey;
    }

    public byte[] getPeerKeyExchangePublicKey() {
        return peerKeyExchangePublicKey;
    }

    private native long createNewLengthMessageNativeForDecryption(byte[] header, byte[] receiveKey);

    private native long createNewLengthMessageNativeForEncryption(byte[] transmitKey);

    private native void cleanLengthMessageNative(long handle);


    public LengthMessageCrypto createNewLengthMessageForDecryption(byte[] header) throws GeneralSecurityException {
        if (header == null || header.length != LengthMessageCrypto.getHeaderLength()) {
            throw new IllegalArgumentException("part 1 must be exactly " + LengthMessageCrypto.getHeaderLength() + " bytes");
        }
        long lengthMessageNativeHandle = createNewLengthMessageNativeForDecryption(header, receiveKey);
        if (lengthMessageNativeHandle == 0) {
            throw new GeneralSecurityException("unable to create new length message");
        }
        return new LengthMessageCrypto(this, lengthMessageNativeHandle, LengthMessageCrypto.Purpose.DECRYPTION);
    }

    public LengthMessageCrypto createNewLengthMessageForEncryption() throws GeneralSecurityException {
        long lengthMessageNativeHandle = createNewLengthMessageNativeForEncryption(transmitKey);
        if (lengthMessageNativeHandle == 0) {
            throw new GeneralSecurityException("unable to create new length message");
        }
        return new LengthMessageCrypto(this, lengthMessageNativeHandle, LengthMessageCrypto.Purpose.ENCRYPTION);
    }

    void cleanLengthMessage(LengthMessageCrypto crypto) {
        cleanLengthMessageNative(crypto.nativeHandle);
        crypto.nativeHandle = 0;
    }
}
