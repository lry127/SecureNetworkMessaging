package us.leaf3stones.snm.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class ProofOfWorkClientTest {

    @Test
    void ensureTimeLimitIsHonored() {
        short computeAtMost = 300;
        Thread bomb = new Thread(() -> {
            try {
                Thread.sleep(computeAtMost * 2);
            } catch (InterruptedException e) {
                return;
            }
            fail("timer expired");
        });
        bomb.start();
        Long res = new ProofOfWorkClient(null).computeNonceBlockingAtMost(computeAtMost, (short) 0, (short) 20);
        assertNull(res);
        bomb.interrupt();
    }
}