package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.message.Message;

import java.nio.ByteBuffer;

public class ArithmeticMessage extends Message {
    public static class Operator {
        public static final byte ADD = 1;
        public static final byte MINUS = 2;
        public static final byte MOD = 3;
    }

    private byte operator;
    private long operand1;
    private long operand2;

    public ArithmeticMessage(byte operator, long operand1, long operand2) {
        this.operator = operator;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public ArithmeticMessage(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    protected int getTypeIdentifier() {
        // must be globally unique
        return ArithmeticMessageDecoder.MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE;
    }

    @Override
    protected int peekDataSize() {
        // before sending message, we need to tell the framework how many bytes we intend to send
        // so that it'll save enough buffer for us
        return Byte.BYTES + Long.BYTES * 2; // operator: 1 bytes + 2 operands * 8 bytes/operand
    }

    @Override
    protected void serialize(ByteBuffer buf) {
        // serialize the data we want to send to peer
        buf.put(operator);
        buf.putLong(operand1);
        buf.putLong(operand2);
    }

    @Override
    protected void constructMessage(ByteBuffer buf) throws Exception {
        // this happens at the receiving side, construct the original message from buffer
        operator = buf.get();
        operand1 = buf.getLong();
        operand2 = buf.getLong();
    }

    public String execute() {
        long result = 0;
        String operatorString;
        //noinspection EnhancedSwitchMigration
        switch (operator) {
            case Operator.ADD:
                result = operand1 + operand2;
                operatorString = "plus";
                break;
            case Operator.MINUS:
                result = operand1 - operand2;
                operatorString = "minus";
                break;
            case Operator.MOD:
                result = operand1 % operand2;
                operatorString = "modulo";
                break;
            default:
            throw new RuntimeException("undefined operator: " + operator);
        }
        return String.format("%d %s %d is %d\n", operand1, operatorString, operand2, result);
    }

    public static ArithmeticMessage additionMessage(long operand1, long operand2) {
        return new ArithmeticMessage(Operator.ADD, operand1, operand2);
    }

    public static ArithmeticMessage subtractionMessage(long operand1, long operand2) {
        return new ArithmeticMessage(Operator.MINUS, operand1, operand2);
    }

    public static ArithmeticMessage moduloMessage(long operand1, long operand2) {
        return new ArithmeticMessage(Operator.MOD, operand1, operand2);
    }

}
