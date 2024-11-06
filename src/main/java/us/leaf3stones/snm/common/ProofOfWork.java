package us.leaf3stones.snm.common;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ProofOfWork {
    public static long doWork(long base, int difficulty) {
        long nonce = 0;
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (true) {
            String hashAttempt = calculateHash(base, nonce);
            if (hashAttempt.substring(0, difficulty).equals(target)) {
                System.err.println("nonce is " + nonce);
                return nonce;
            }
            nonce++;
        }
    }

    public static boolean checkWork(long base, long nonce, int difficulty) {
        String hash = calculateHash(base, nonce);
        String target = new String(new char[difficulty]).replace('\0', '0');
        return hash.substring(0, difficulty).equals(target);
    }

    private static String calculateHash(long base, long nonce) {
        byte[] input = ByteBuffer.allocate(Long.BYTES * 2).putLong(base).putLong(nonce).array();
        return computeSha256(input);
    }

    private static String computeSha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
