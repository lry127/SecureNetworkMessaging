package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.Message;

public class ArithmeticOperationHandler extends MessageHandler {
    public ArithmeticOperationHandler(HttpSecPeer peer) {
        super(peer);
    }

    @Override
    public void takeOver() throws Exception {
        //noinspection InfiniteLoopStatement
        while (true) {
            if (!(peer.readMessage() instanceof ArithmeticMessage arithmeticMsg)) {
                throw new RuntimeException("can only handle arithmetic message");
            }
            String executedCalculation = arithmeticMsg.execute();
            Message response = ArithmeticResponseMessage.newInstance(executedCalculation);
            peer.sendMessage(response);
        }
    }
}
