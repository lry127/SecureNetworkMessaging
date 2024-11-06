package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.client.HttpSecClient;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.message.Message;

import java.util.Random;

public class ClientMain {
    public static void main(String[] args) throws Exception{
        HttpSecClient client = new HttpSecClient("localhost", 5000, new ArithmeticMessageDecoder(new BaseMessageDecoder()));
        Random r = new Random();
        for (int i = 0; i < 10; ++i) {
            Message m;
            int op = r.nextInt(0, 3);
            long operand1 = r.nextLong(100, 10000);
            long operand2 = r.nextLong(100, 10000);
            //noinspection EnhancedSwitchMigration
            switch (op) {
                case 0:
                    m = ArithmeticMessage.additionMessage(operand1, operand2);
                    break;
                case 1:
                    m = ArithmeticMessage.subtractionMessage(operand1, operand2);
                    break;
                case 2:
                    m = ArithmeticMessage.moduloMessage(operand1, operand2);
                    break;
                default:
                    throw new AssertionError("impossible");
            }
            client.sendMessage(m);
            ArithmeticResponseMessage response = (ArithmeticResponseMessage) client.readMessage();
            System.err.print(response.getMessage());
        }
        System.err.println("done");
    }
}
