package us.leaf3stones.snm.crypto;

public class CryptoInitializer {
    private static boolean initialized = false;

    public static synchronized void initNativeCrypto() {
        if (initialized) {
            return;
        }
        CustomNativeLibInit.init();
        initialized = true;
    }
}
