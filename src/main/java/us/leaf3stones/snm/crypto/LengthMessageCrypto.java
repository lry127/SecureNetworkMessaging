package us.leaf3stones.snm.crypto;

import java.security.GeneralSecurityException;

public class LengthMessageCrypto {
    public static final String serverKeyExchangePublicKey = "oOXyVeq7p1ooKvLbbcGO+t2Y+D4qiSbGeCi0Ug7vDlg=";
    public static final String serverKeyExchangePrivateKey = "NMQrEmJXxf+0rQMgsux27cjP6vmW678ArBgQkAS8ytU=";

    private static final int CRYPTO_NONCE_BYTES = 24;
    private static final int CRYPTO_MAC_BYTES = 16;
    private final NegotiatedCryptoNative backingCrypto;
    private final Purpose purpose;
    long nativeHandle;

    LengthMessageCrypto(NegotiatedCryptoNative backingCrypto, long nativeHandle, Purpose purpose) {
        this.backingCrypto = backingCrypto;
        this.nativeHandle = nativeHandle;
        this.purpose = purpose;
    }

    public static int getHeaderLength() {
        return CRYPTO_NONCE_BYTES + CRYPTO_MAC_BYTES + Integer.BYTES;
    }

    public static int getFullEncryptionOverhead() {
        return getHeaderLength() + CRYPTO_MAC_BYTES;
    }

    public static native int getTotalInstanceCount();

    private native byte[] encryptNative(long dataPtr, long size);

    private native byte[] decryptNative(byte[] data);

    public native int getEncryptedBodySize();

    public byte[] encrypt(NativeBuffer buffer) throws GeneralSecurityException {
        if (purpose != Purpose.ENCRYPTION) {
            throw new IllegalStateException("this message is not meant for encryption");
        }

        try {
            ensureValid();
            byte[] encrypted = encryptNative(buffer.getHandle(), buffer.size());
            if (encrypted == null) {
                throw new GeneralSecurityException("failed to encrypt message");
            }
            return encrypted;
        } finally {
            clean();
            buffer.clean();
        }
    }

    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data can't be empty");
        }
        NativeBuffer buffer = new NativeBuffer(data.length);
        buffer.wrapAsByteBuffer().put(data);
        return encrypt(buffer);
    }

    public byte[] decrypt(byte[] ciphertext) throws GeneralSecurityException {
        if (purpose != Purpose.DECRYPTION) {
            throw new IllegalStateException("this message is not meant for decryption");
        }

        try {
            ensureValid();

            if (ciphertext == null || ciphertext.length != getEncryptedBodySize()) {
                throw new IllegalArgumentException("encrypted body length must match exactly with getEncryptedBodySize()");
            }
            byte[] decrypted = decryptNative(ciphertext);
            if (decrypted == null) {
                throw new GeneralSecurityException("failed to decrypt message");
            }
            return decrypted;
        } finally {
            clean();
        }
    }

    private void ensureValid() {
        if (nativeHandle == 0) {
            throw new IllegalStateException("this message is not valid. one object can only be used at most once");
        }
    }

    public void clean() {
        if (nativeHandle == 0) {
            return;
        }
        backingCrypto.cleanLengthMessage(this);
    }

    enum Purpose {
        ENCRYPTION,
        DECRYPTION
    }

}
