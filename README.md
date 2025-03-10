# DEPRECATED
Use [Netty](https://netty.io/) instead, it's excellent.

# SecureNetworkMessaging

A java library that helps you write your own secure network messaging system effortlessly.

## Features

- [x] custom messages

- [x] secure transport (based on [mTLS](https://en.wikipedia.org/wiki/Mutual_authentication))

- [x] rate throttling

- [x] easy to use api

- [x] messages with different priority

## Getting Started & Tutorial

Here we'll implement some simple protocols, in just a few lines.

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
       }
   }
   ```
   
   Congrats! Server now knows how to echo back to client. It's time to actually run the server.

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
   
   The client will print `response: hi, SecureNetworkMessaging!` in the console and exit if everything is up. You can find the source code in `src/main/java/us/leaf3stones/snm/demo/echo`

5. Congrats! 
   
   You've created your echo server in just a few lines. The library is highly flexible. You can explore it further by either following next tutorial or diving into the source code.

#### Example #2: Remote Calculator

In this tutorial, we'll implement a remote calculator using SecureNetworkMessaging library (source code located at `src/main/java/us/leaf3stones/snm/demo/arithmetic`). You'll learn...

- How to define your own messages

- How to prevent your server from abuse (rate limiting api)
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

2. Defining arithmetic response message. This is message sent in response to `ArithmeticMessage` at the server side.
   
   Note that while it's possible to return a `GeneralResponseMessage` containing the result,  I'll instead define a new type of message here to show you how to send messages with *variable length*.
   
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
   
   import org.slf4j.Logger;
   import org.slf4j.LoggerFactory;
   import us.leaf3stones.snm.common.HttpSecPeer;
   import us.leaf3stones.snm.handler.MessageHandler;
   import us.leaf3stones.snm.message.Message;
   import us.leaf3stones.snm.common.NetIOException;
   
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
   ```

5. Bringing up server. (If you run into any error related to native library, please refer to tutorial 1 and learn how to build it)
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.auth.AuthenticationChain;
   import us.leaf3stones.snm.auth.NonceAuthenticator;
   import us.leaf3stones.snm.common.HttpSecPeer;
   import us.leaf3stones.snm.handler.HandlerFactory;
   import us.leaf3stones.snm.handler.MessageHandler;
   import us.leaf3stones.snm.message.BaseMessageDecoder;
   import us.leaf3stones.snm.server.HttpSecServer;
   import us.leaf3stones.snm.server.HttpSecServerBuilder;
   
   
   public class ServerMain {
       public static void main(String[] args) throws Exception {
           HttpSecServerBuilder builder = new HttpSecServerBuilder();
           builder.setPort(5000);
           builder.setHandlerFactory(new HandlerFactory() {
               @Override
               public MessageHandler createRequestHandler(HttpSecPeer peer) {
                   return new ArithmeticOperationHandler(peer);
               }
           });
           // we used BaseMessageDecoder internally, if you have your own decoder, chain it as parent
           builder.setMessageDecoder(new ArithmeticMessageDecoder(new BaseMessageDecoder()));
           builder.setRateLimitingPolicy(new CalculatorRateLimiting());
           // fight against replay attack by using a nonce
           builder.setAuthChain(new AuthenticationChain(NonceAuthenticator.class));
           HttpSecServer server = builder.build();
           server.accept(true);
       }
   }
   
   ```

6. Run the client
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.client.HttpSecClient;
   import us.leaf3stones.snm.client.NonceAuthClient;
   import us.leaf3stones.snm.message.BaseMessageDecoder;
   import us.leaf3stones.snm.message.Message;
   import us.leaf3stones.snm.common.NetIOException;
   
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
   ```
   
   We call the `enableKeepAlive` function so that the client will send a dummy message to server every 10 seconds. This is useful to make sure the TCP connection is not closed by the server due to inactivity.
   
   Example input and output
   
   > ```
   > 3490284 + 234723
   > 3490284 plus 234723 is 3725007
   > 
   > 39248 - 23482
   > 39248 minus 23482 is 15766
   > 
   > 32 - 3248
   > 32 minus 3248 is -3216
   > 
   > 1004520 % 24399
   > 1004520 modulo 24399 is 4161
   > 
   > ^D
   > done
   > ```

7. Rate limiting
   
   To set a limit on how fast any client *with the same IP* can access our service, we need to implement the `RateLimitingPolicy` interface
   
   ```java
   public interface RateLimitingPolicy {
       int getRefreshIntervalMillis();
       void onRefresh(Map<Integer, AccessLog> accessMap, long currTime);
       int getWaitingTimeFor(int ip, AccessLog accessLog);
   }
   
   public static class AccessLog {
       public List<Long> accesses = new ArrayList<>();
   }
   ```
   
   1. `getRefreshIntervalMillis()` determines the interval of calling `onRefresh()` by the framework
   
   2. `getWaitingTimeFor()`  is called whenever a client want to connect to the server, either return an integer (in milliseconds) so that the service will be delayed for that amount of time, or throw a `TooManyRequest` exception, resulting the framework close the connection immediately (by sending TCP RST to client)
   
   Here we'll implement a simple policy that there's no delay as long as the client don't access our server more than 3 times within 30 seconds. If the client tries to do so, the server will immediately close the connection.
   
   ```java
   package us.leaf3stones.snm.demo.arithmetic;
   
   import us.leaf3stones.snm.rate.RateLimiting;
   
   import java.util.ArrayList;
   import java.util.Map;
   
   public class CalculatorRateLimiting implements RateLimiting.RateLimitingPolicy {
       private static final int REFRESH_INTERVAL_MILLIS = 10_000; // 10 sec
       private static final int LOG_RESET_INTERVAL_MILLIS = 30_000; // 30 sec
   
       @Override
       public int getRefreshIntervalMillis() {
           return REFRESH_INTERVAL_MILLIS;
       }
   
       @Override
       public void onRefresh(Map<Integer, RateLimiting.AccessLog> accessMap, long currTime) {
           long expireMin = currTime - LOG_RESET_INTERVAL_MILLIS;
           ArrayList<Integer> expired = new ArrayList<>();
           for (Map.Entry<Integer, RateLimiting.AccessLog> entry : accessMap.entrySet()) {
               RateLimiting.AccessLog log = entry.getValue();
               if (log.accesses.isEmpty()) {
                   expired.add(entry.getKey());
                   continue;
               }
               log.accesses.removeIf(accessedTime -> accessedTime < expireMin);
           }
           expired.forEach(accessMap::remove);
       }
   
       @Override
       public int getWaitingTimeFor(int ip, RateLimiting.AccessLog accessLog) {
           if (accessLog.accesses.size() < 3)  {
               accessLog.accesses.add(System.currentTimeMillis());
               return 0;
           }
           throw new RateLimiting.TooManyRequestException("this ip requested more than 3 connections within 30 secs, " +
                   "rejecting new connections");
       }
   }
   ```
   
   Set the policy in the server builder
   
   ```java
   builder.setRateLimitingPolicy(new CalculatorRateLimiting());
   ```
   
   Now try to connect to the server. The first 3 accesses returns the results normally, but when trying to access it the forth time, exception will occur,  indicating our policy is working properly.
   
   > ```
   > Exception in thread "main" java.net.SocketException: Connection reset by peer
   >     at java.base/sun.nio.ch.SocketDispatcher.write0(Native Method)
   >     at java.base/sun.nio.ch.SocketDispatcher.write(SocketDispatcher.java:62)
   >     at java.base/sun.nio.ch.NioSocketImpl.tryWrite(NioSocketImpl.java:394)
   >     at java.base/sun.nio.ch.NioSocketImpl.implWrite(NioSocketImpl.java:410)
   >     at java.base/sun.nio.ch.NioSocketImpl.write(NioSocketImpl.java:440)
   >     at java.base/sun.nio.ch.NioSocketImpl$2.write(NioSocketImpl.java:819)
   >     at java.base/java.net.Socket$SocketOutputStream.write(Socket.java:1195)
   >     at java.base/java.io.OutputStream.write(OutputStream.java:124)
   >     at us.leaf3stones.snm.crypto.CryptoNegotiation.negotiateAsClient(CryptoNegotiation.java:36)
   >     at us.leaf3stones.snm.common.HttpSecPeer.tryToNegotiateCryptoInfo(HttpSecPeer.java:43)
   >     at us.leaf3stones.snm.client.HttpSecClient.<init>(HttpSecClient.java:14)
   >     at us.leaf3stones.snm.demo.arithmetic.ClientMain.main(ClientMain.java:11)
   > ```

        
