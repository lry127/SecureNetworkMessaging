package us.leaf3stones.snm.client;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.common.ProofOfWork;
import us.leaf3stones.snm.message.AuthenticationMessage;
import us.leaf3stones.snm.message.AuthenticationResponseMessage;
import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.POWAuthenticationMessage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class ProofOfWorkClient {
    protected final HttpSecPeer client;

    public ProofOfWorkClient(HttpSecPeer client) {
        this.client = client;
    }

    public void authenticateToServer() throws IOException, SecurityException {
        Message authRequestMessage = client.readMessage();
        if (!(authRequestMessage instanceof POWAuthenticationMessage)) {
            throw new IllegalStateException("wtf? server don't require auth and we consumed an message?!");
        }
        //noinspection PatternVariableCanBeUsed
        POWAuthenticationMessage authMsg = (POWAuthenticationMessage) authRequestMessage;

        Long nonce = computeNonceBlockingAtMost(authMsg.getMinBypassMillis(), authMsg.getBase(), authMsg.getSpecification());
        AuthenticationResponseMessage responseMsg;
        if (nonce != null) {
            responseMsg = AuthenticationResponseMessage.newInstance(false, nonce);
        } else {
            responseMsg = AuthenticationResponseMessage.newInstance(true, -1);
        }
        client.sendMessage(responseMsg);
    }

    /* visible for testing */ Long computeNonceBlockingAtMost(short waitAtMost, long base, short difficulty) {
        long minBypassWait = waitAtMost != -1 ? waitAtMost : Long.MAX_VALUE;
        AtomicReference<Long> nonce = new AtomicReference<>();
        Thread timeoutNotifier = new Thread(() -> {
            try {
                Thread.sleep(minBypassWait);
                synchronized (ProofOfWorkClient.this) {
                    ProofOfWorkClient.this.notify();
                }
            } catch (InterruptedException ignored) {
            }
        });

        Thread proofOfWorkThread = new Thread(() -> {
            nonce.set(ProofOfWork.doWork(base, difficulty));
            synchronized (ProofOfWorkClient.this) {
                ProofOfWorkClient.this.notify();
            }
        });

        try {
            synchronized (ProofOfWorkClient.this) {
                proofOfWorkThread.start();
                timeoutNotifier.start();
                ProofOfWorkClient.this.wait();
            }
        } catch (InterruptedException ignored) {
        } finally {
            timeoutNotifier.interrupt();
            proofOfWorkThread.interrupt();
        }

        return nonce.get();
    }

}
