package us.leaf3stones.snm.common;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProofOfWorkTest {
    @Test
    void functionalityTest() {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            long base = random.nextLong();
            long computedNonce = ProofOfWork.doWork(base, 4);
            assertTrue(ProofOfWork.checkWork(base, computedNonce, 4));
        }
    }
}