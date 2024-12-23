package us.leaf3stones.snm.demo.arithmetic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.common.NetIOException;
import us.leaf3stones.snm.handler.MessageHandler;
import us.leaf3stones.snm.message.Message;

import java.io.IOException;

public class ArithmeticOperationHandler extends MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ArithmeticOperationHandler.class);

    public ArithmeticOperationHandler(HttpSecPeer peer) {
        super(peer);
    }

    @Override
    public void takeOver() throws Exception {
        while (true) {
            try {
                if (!(peer.readMessage() instanceof ArithmeticMessage arithmeticMsg)) {
                    throw new RuntimeException("can only handle arithmetic message");
                }
                String executedCalculation = arithmeticMsg.execute();
                Message response = ArithmeticResponseMessage.newInstance(executedCalculation);
                peer.sendMessage(response);
            } catch (NetIOException netIOException) {
                if (!netIOException.isAbnormalIOException) {
                    logger.info("client closed the connection cleanly");
                    break;
                } else {
                    throw new IOException(netIOException);
                }
            }
        }
    }
}
