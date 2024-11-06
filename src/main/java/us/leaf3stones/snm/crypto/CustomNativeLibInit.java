package us.leaf3stones.snm.crypto;

/**
 * load your native library according your machine arch.
 * the class name and {@link #init()} method should not be changed <br>
 * <strong>If you're going to modify it, run the following command
 * at the root project directory to prevent the submission to git</strong><br>
 * <code>git update-index --assume-unchanged src/main/java/us/leaf3stones/snm/crypto/CustomNativeLibInit.java</code>
 */
public class CustomNativeLibInit {
    public static void init() {
        System.load("/home/ubuntu/CLionProjects/sodium-play/cmake-build-debug/libcrypto_jni.so");
        if (NegotiatedCryptoNative.initSodiumLibrary() != 0) {
            throw new Error("can't init sodium lib");
        }
    }
}
