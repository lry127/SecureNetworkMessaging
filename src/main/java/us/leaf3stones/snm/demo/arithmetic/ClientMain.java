package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.client.HttpSecClient;
import us.leaf3stones.snm.client.NonceAuthClient;
import us.leaf3stones.snm.common.NetIOException;
import us.leaf3stones.snm.message.BaseMessageDecoder;
import us.leaf3stones.snm.message.Message;

import java.util.Scanner;

public class ClientMain {
    private static HttpSecClient client;

    public static void main(String[] args) throws Exception {
        client = new HttpSecClient("localhost", 5000, new ArithmeticMessageDecoder(new BaseMessageDecoder()));
        new NonceAuthClient(client).authenticateToServer();
        client.enableKeepAlive(10_000);
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                processLine(scanner.nextLine());
            }
        } finally {
            client.shutdown();
        }
        System.err.println("done");
    }

    private static void processLine(String line) throws NetIOException {
        char operator;
        long operand1;
        long operand2;
        int idx;
        if ((idx = line.indexOf("+")) != -1) {
            operator = '+';
        } else if ((idx = line.indexOf("-")) != -1) {
            operator = '-';
        } else if ((idx = line.indexOf("%")) != -1) {
            operator = '%';
        } else {
            System.err.println("ill-formated expression. type expression in format like a + b or a - b or a % b.");
            return;
        }
        String operand1String;
        String operand2String;
        try {
            operand1String = line.substring(0, idx).trim();
            operand2String = line.substring(idx + 1).trim();
        } catch (Exception e) {
            System.err.println("ill-formated expression. type expression in format like a + b or a - b or a % b.");
            return;
        }
        try {
            operand1 = Long.parseLong(operand1String);
        } catch (NumberFormatException e) {
            System.err.println("failed to parse operator 1 to a long: \"" + operand1String + "\"");
            return;
        }
        try {
            operand2 = Long.parseLong(operand2String);
        } catch (NumberFormatException e) {
            System.err.println("failed to parse operator 2 to a long: \"" + operand1String + "\"");
            return;
        }

        Message message = prepareMessage(operator, operand1, operand2);
        client.sendMessage(message);
        ArithmeticResponseMessage response = (ArithmeticResponseMessage) client.readMessage();
        System.err.println(response.getMessage());
    }

    private static Message prepareMessage(char operator, long operand1, long operand2) {
        //noinspection EnhancedSwitchMigration
        switch (operator) {
            case '+':
                return ArithmeticMessage.additionMessage(operand1, operand2);
            case '-':
                return ArithmeticMessage.subtractionMessage(operand1, operand2);
            case '%':
                return ArithmeticMessage.moduloMessage(operand1, operand2);
            default:
                throw new AssertionError("can't go here");
        }
    }
}
