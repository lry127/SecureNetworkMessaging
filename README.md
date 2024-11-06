# SecureNetworkMessaging

A java library that helps you write your own secure network messaging system effortlessly.

## Features

- custom messages

- strong encryption by default (based on [libsodium](https://doc.libsodium.org/))

- rate throttling

- easy to use api

## Getting Started & Tutorial

Here we'll implement some simple protocols, in just a few lines

#### Example #1: A simple echo server

1. Setting up a handler on server side by extending the `MessageHandler` class.
   
   ```java
   public class EchoMessageHandler extends MessageHandler {
       public EchoMessageHandler(HttpSecPeer peer) {
           super(peer);
       }
   
       @Override
       public void takeOver() throws Exception {
   
       }
   }
   ```
   
   Now, we can read messages from client and send response back to it. Here, we'll use the predefined `GeneralPayloadMessage` class to carry payload.
   
   ```java
   public class EchoMessageHandler extends MessageHandler {
       public EchoMessageHandler(HttpSecPeer peer) {
           super(peer);
       }
   
       @Override
       public void takeOver() throws Exception {
           Message received = peer.readMessage(); // read a message from client
           if (!(received instanceof GeneralPayloadMessage payloadMsg)) {
               throw new RuntimeException("client sent unexpected type of message");
           }
           if (!"echo".equals(payloadMsg.getName())) {
               throw new RuntimeException("we expect echo message from client");
           }
           String message = payloadMsg.getPayloadAsString(); // client message
           Message response = GeneralPayloadMessage.newInstance("echo_response", message); // preparing response
           peer.sendMessage(response); // send it back to client
           peer.shutdown();
       }
   }
   ```
   
   Congrats! Server now knows how to echo back to client. But how do we bring up the server?

2. Running the server
   
   We can use the `HttpSecServerBuilder` class the build and run a server.
   
   ```java
   public class ServerMain {
       public static void main(String[] args) throws IOException {
           HttpSecServerBuilder builder = new HttpSecServerBuilder();
           builder.setPort(5000); // 1
           builder.setHandlerFactory(new HandlerFactory() {
               @Override
               public MessageHandler createRequestHandler(HttpSecPeer peer) {
                   return new EchoMessageHandler(peer);
               }
           }); // 2
           builder.setMessageDecoder(new BaseMessageDecoder()); // 3
           HttpSecServer server = builder.build();
           server.accept(true); // 4
       }
   }
   ```
   
   - // 1
     
     set listening port
   
   - // 2
     
     we want to use the `EchoMessageHandler` handler we just created to handle client connections
   
   - // 3
     
     since we used the `GeneralPayloadMessage`, which can be parsed by a predefined `BaseMessageDecoder`, set it here. You can parse custom messages by extending the `MessageDecoder` class.
   
   - // 4
     
     block the current thread and run the server

3. Creating a Client
   
   ```java
   public class ClientMain {
      public static void main(String[] args) throws Exception{
          HttpSecClient client = new HttpSecClient("localhost", 5000, new BaseMessageDecoder());
          GeneralPayloadMessage echoRequest = GeneralPayloadMessage.newInstance("echo", "hi, SecureNetworkMessaging!");
          client.sendMessage(echoRequest);
          GeneralPayloadMessage echoResponse = (GeneralPayloadMessage) client.readMessage();
          System.err.println("response: " + echoResponse.getPayloadAsString());
      }
   }
   ```

4. Run the server first and then run the client.
   
   You'll need to build the jni crypto library project first to run the java program. For more information, check out the [jni project](https://github.com/lry127/secure_network_messaging_crypto_lib).
   
   After a successful build, follow instruction in `src/main/java/us/leaf3stones/snm/crypto/CustomNativeLibInit.java` and modify that file.
   
   The client will print `response: hi, SecureNetworkMessaging!` in the console and exit if everything is up. You can find the source code in `src/main/java/us/leaf3stones/snm/demo/echo`

5. Congrats! 
   
   You've created your echo server in just a few lines. The library is highly flexible. You can explore it further by either following next tutorial or diving into the source code.

#### Example #2: Remote Calculator

In this tutorial, we'll implement a remote calculator using SecureNetworkMessaging library. You'll learn...

- How to define your own messages

- How to avoid flooding your server (rate limiting api)
1. Defining arithmetic request message. This is the message client sends to server to request calculation
   
   ```java
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
   
   ```
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.message.Message;
   import us.leaf3stones.snm.message.MessageDecoder;
   
   import java.nio.ByteBuffer;
   import java.util.Set;
   
   public class ArithmeticMessageDecoder extends MessageDecoder {
       public static class MessageTypeIdentifiers {
           public static int TYPE_ARITHMETIC_MESSAGE = 1000;
       }
   
       public ArithmeticMessageDecoder(MessageDecoder parent) {
           super(parent);
       }
   
       @Override
       protected Message convert(int messageId, ByteBuffer messageBody) throws DecodeException {
           if (messageId == MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE) {
               return new ArithmeticMessage(messageBody);
           }
           throw new AssertionError("can't go here");
       }
   
       @Override
       protected Set<Integer> getConvertableMessageIds() {
           return Set.of(MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE);
       }
   }
   
   ```
   
   notable features:
   
   - all messages should extend the `Message` class and return a globally unique identifier in method `int getTypeIdentifier()`
   
   - all messages must have a constructor that calls the super constructor `Message(ByteBuffer buffer)` in order to recover it at the receiving side
   
   - all messages should return the message size in the `int peekDataSize()` method, this will be call at the sending side to prepare the buffer
   
   - in `serialize(ByteBuffer buf)` method, write your message. the size should match *exactly* with the value you return from `peekDataSize()` (this method is called at the sending side)
   
   - in `constructMessage(ByteBuffer buf)` method, recover your message. you can assume it's large enough to read all bytes you need. (this method is called at the receiving side)
   
   - you must have a `MessageDecoder` that recognize this type of message. 

2. Defining arithmetic response message. This is message in respond to `ArithmeticMessage` at the server side.
   
   In theory, you can return a `GeneralResponseMessage` containing the result, but I'll define a new type of message here to show you how to send messages with *variable length*.
   
   To demonstrate this, we return a text message directly to client in readable form. (so that the length is not fixed)
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.message.Message;
   
   import java.nio.ByteBuffer;
   import java.nio.charset.StandardCharsets;
   
   public class ArithmeticResponseMessage extends Message {
       private byte[] message;
       
       public ArithmeticResponseMessage(byte[] message) {
           this.message = message;
       }
       
       public ArithmeticResponseMessage(ByteBuffer buffer) {
           super(buffer);
       }
       
       @Override
       protected int getTypeIdentifier() {
           return ArithmeticMessageDecoder.MessageTypeIdentifiers.TYPE_ARITHMETIC_RESPONSE_MESSAGE;
       }
   
       @Override
       protected int peekDataSize() {
           return lengthWithHeader(message);
       }
   
       @Override
       protected void serialize(ByteBuffer buf) {
           sizedPut(message, buf);
       }
   
       @Override
       protected void constructMessage(ByteBuffer buf) throws Exception {
           message = sizedRead(buf);
       }
       
       public String getMessage() {
           return new String(message, StandardCharsets.UTF_8);
       }
       
       public static ArithmeticResponseMessage newInstance(String response) {
           return new ArithmeticResponseMessage(response.getBytes(StandardCharsets.UTF_8));
       }
   }
   
   ```
   
   notable features:
   
   - to put a variable length byte[] into the message, you can use the following helper methods defined in `Message` class
     
     - `lengthWithHeader(byte[] msg)` compute the space you need to store a variable length `msg`, **never return `message.length` directly in `peekDataSize()` method when dealing with variable length message**
     
     - when writing a variable length `byte[]` to buffer, call `sizedPut(byte[] msg, ByteBuffer buf)` defined in `Message` class. Again, **never call `buf.put(msg)` directly**
     
     - when reading a variable length `byte[]` from buffer, call `sizedRead(ByteBuffer buf)` defined in `Message` class
     
     - *under the hood:*
       
       1. `sizedPut(byte[] msg, ByteBuffer buf)` will put `msg.length`(`Integer.BYTES` bytes) to `buf` before putting `msg` so that the `sizedRead(ByteBuffer buf)` knows how many bytes the variable length message `msg` has
       
       2. `lengthWithHeader(byte[] msg)` will return `msg.length + Integer.BYTES` , because `sizedPut` method will write the extra `Integer.BYTES` bytes as header

3. Now update the `ArithmeticMessageDecoder` class so that it can recognize  `ArithmeticResponseMessage`
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.message.Message;
   import us.leaf3stones.snm.message.MessageDecoder;
   
   import java.nio.ByteBuffer;
   import java.util.Set;
   
   public class ArithmeticMessageDecoder extends MessageDecoder {
       public static class MessageTypeIdentifiers {
           public static int TYPE_ARITHMETIC_MESSAGE = 1000;
           public static int TYPE_ARITHMETIC_RESPONSE_MESSAGE = 1001;
       }
   
       public ArithmeticMessageDecoder(MessageDecoder parent) {
           super(parent);
       }
   
       @Override
       protected Message convert(int messageId, ByteBuffer messageBody) throws DecodeException {
           if (messageId == MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE) {
               return new ArithmeticMessage(messageBody);
           } else if (messageId == MessageTypeIdentifiers.TYPE_ARITHMETIC_RESPONSE_MESSAGE) {
               return new ArithmeticResponseMessage(messageBody);
           }
           throw new AssertionError("can't go here");
       }
   
       @Override
       protected Set<Integer> getConvertableMessageIds() {
           return Set.of(MessageTypeIdentifiers.TYPE_ARITHMETIC_MESSAGE,
                   MessageTypeIdentifiers.TYPE_ARITHMETIC_RESPONSE_MESSAGE);
       }
   }
   
   ```

4. Writing handler class so that the server knows how to handle calculation requests
   
   ```java
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
   
   ```

5. Bringing up server. (If you run into any error related to native library, please refer to tutorial 1 and learn how to build it)
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.common.HttpSecPeer;
   import us.leaf3stones.snm.handler.HandlerFactory;
   import us.leaf3stones.snm.handler.MessageHandler;
   import us.leaf3stones.snm.message.BaseMessageDecoder;
   import us.leaf3stones.snm.server.HttpSecServer;
   import us.leaf3stones.snm.server.HttpSecServerBuilder;
   
   import java.io.IOException;
   
   public class ServerMain {
       public static void main(String[] args) throws IOException {
           HttpSecServerBuilder builder = new HttpSecServerBuilder();
           builder.setPort(5000);
           builder.setHandlerFactory(new HandlerFactory() {
               @Override
               public MessageHandler createRequestHandler(HttpSecPeer peer) {
                   return new ArithmeticOperationHandler(peer);
               }
           });
           // though we didn't use any predefined message, we can still include the
           // base decoder for easier migration if we later changed our minds and use
           // a predefined message
           builder.setMessageDecoder(new ArithmeticMessageDecoder(new BaseMessageDecoder()));
           HttpSecServer server = builder.build();
           server.accept(true);
       }
   }
   
   ```

6. Run the client
   
   ```java
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
   
   ```
   
   Example output
   
   > ```
   > 252 plus 9068 is 9320
   > 6164 minus 1487 is 4677
   > 4516 plus 6494 is 11010
   > 8232 plus 5018 is 13250
   > 7085 minus 1495 is 5590
   > 9417 plus 1687 is 11104
   > 7153 plus 3206 is 10359
   > 4707 modulo 2975 is 1732
   > 713 modulo 2096 is 713
   > 3528 minus 1161 is 2367
   > done
   > ```

7. Rate limiting
   
   Coming soon. Read the classes inside `rate` package to learn more.
