package us.leaf3stones.snm.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.crypto.LengthMessageCrypto;
import us.leaf3stones.snm.crypto.NegotiatedCryptoNative;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public class MessageFactory {
    private static final int MAX_ACCEPTABLE_MESSAGE_LENGTH = 65535;
    private static final Logger logger = LoggerFactory.getLogger(MessageFactory.class);

    private final NegotiatedCryptoNative crypto;
    private final MessageDecoder decoder;

    public MessageFactory(NegotiatedCryptoNative crypto, MessageDecoder decoder) {
        this.crypto = crypto;
        this.decoder = decoder;
    }

    public Message parseMessage(InputStream in) throws IOException {
        LengthMessageCrypto lengthMessage = null;
        try {
            byte[] encLengthMsg = in.readNBytes(LengthMessageCrypto.getHeaderLength());
            if (encLengthMsg.length != LengthMessageCrypto.getHeaderLength()) {
                throw new EOFException("failed to read: " + encLengthMsg.length + " bytes");
            }
            lengthMessage = crypto.createNewLengthMessageForDecryption(encLengthMsg);
            if (lengthMessage == null) {
                throw new GeneralSecurityException("failed to decrypt header from peer");
            }
            long encryptedBodySize = lengthMessage.getEncryptedBodySize();
            if (encryptedBodySize > MAX_ACCEPTABLE_MESSAGE_LENGTH || encryptedBodySize <= 0) {
                throw new GeneralSecurityException("peer sent a message with unacceptable payload. payload len: " + encryptedBodySize);
            }
            byte[] bodyEncrypted = in.readNBytes((int) encryptedBodySize);
            if (bodyEncrypted.length != encryptedBodySize) {
                throw new EOFException("failed to read: " + encryptedBodySize + " bytes");
            }
            byte[] bodyDecrypted = lengthMessage.decrypt(bodyEncrypted);
            if (bodyDecrypted == null) {
                throw new GeneralSecurityException("failed to decrypt message body");
            }
            return decoder.decode(bodyDecrypted);
        } catch (GeneralSecurityException securityException) {
            logger.warn("failed to decrypt message");
            throw new IOException(securityException);
        } finally {
            if (lengthMessage != null) {
                lengthMessage.clean();
            }
        }
    }

    public byte[] serializeMessage(Message message) {
        LengthMessageCrypto lengthMessage = null;
        try {
            lengthMessage = crypto.createNewLengthMessageForEncryption();
            return lengthMessage.encrypt(message.serialize());
        } catch (GeneralSecurityException securityException) {
            logger.error("can't encrypt?");
            throw new RuntimeException(securityException);
        } finally {
            if (lengthMessage != null) {
                lengthMessage.clean();
            }
        }
    }
}
