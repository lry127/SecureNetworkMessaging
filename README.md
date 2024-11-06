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

3.  Creating a Client
   
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

4.  Run the server first and then run the client.
   
   the client will print `response: hi, SecureNetworkMessaging!` in the console and exit.

5. Congrats! 
   
   You've created your echo server in just a few lines. The library is highly flexible. You can explore it further by either following next tutorial or diving into the source code.
