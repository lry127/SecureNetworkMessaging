package us.leaf3stones.snm.common;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import us.leaf3stones.snm.message.GeneralPayloadMessage;
import us.leaf3stones.snm.message.KeepAliveMessage;
import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.NetIOException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class PriorityMessageQueueTest {

    @Test
    void receiverSuccessfullyReceivedWithLargeBufferAndSingleSender() {
        GeneralPayloadMessage msg = GeneralPayloadMessage.newInstance("msg", "data");
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CachedMessageSender sender = new CachedMessageSender();
        int messageMaxBuffer = 50;
        int messageCount = 30;
        PriorityMessageQueue queue = new PriorityMessageQueue(messageMaxBuffer, executor, sender);
        AtomicInteger sendCnt = new AtomicInteger();
        for (int i = 0; i < messageCount; ++i) {
            executor.execute(() -> {
                try {
                    queue.sendMessageWithNormalPriority(msg);
                } catch (NetIOException e) {
                    throw new AssertionError("unexpected exception. " + e);
                }
                sendCnt.incrementAndGet();
            });
        }
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> sendCnt.get() == messageCount);
        assertEquals(30, sender.messages.size());
    }

    @Test
    void receiverSuccessfullyReceivedWithLargeBufferAndMultipleSender() {
        GeneralPayloadMessage msg = GeneralPayloadMessage.newInstance("msg", "data");
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CachedMessageSender sender = new CachedMessageSender();
        int messageMaxBuffer = 5000;
        int messageCount = 3000;
        PriorityMessageQueue queue = new PriorityMessageQueue(messageMaxBuffer, executor, sender);
        AtomicInteger sendCnt = new AtomicInteger();
        for (int i = 0; i < messageCount; ++i) {
            executor.execute(() -> {
                try {
                    queue.sendMessageWithNormalPriority(msg);
                } catch (NetIOException e) {
                    throw new AssertionError("unexpected exception. " + e);
                }
                sendCnt.incrementAndGet();
            });
        }
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> sendCnt.get() == messageCount);

        assertEquals(messageCount, sender.messages.size());
    }

    @Test
    void receiverSuccessfullyReceivedWithLimitedAndMultipleSender() {
        GeneralPayloadMessage msg = GeneralPayloadMessage.newInstance("msg", "data");
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CachedMessageSender receiver = new CachedMessageSender();
        int messageMaxBuffer = 200;
        int messageCount = 3000;
        PriorityMessageQueue queue = new PriorityMessageQueue(messageMaxBuffer, executor, receiver);
        AtomicInteger sendCnt = new AtomicInteger();
        for (int i = 0; i < messageCount; ++i) {
            executor.execute(() -> {
                try {
                    queue.sendMessageWithNormalPriority(msg);
                } catch (NetIOException e) {
                    throw new AssertionError("unexpected exception. " + e);
                }
                sendCnt.incrementAndGet();
            });
        }
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> sendCnt.get() == messageCount);
        assertEquals(messageCount, receiver.messages.size());
    }

    @Test
    void senderIsBlockedWhenMaxBufferLimitIsReached() {
        GeneralPayloadMessage msg = GeneralPayloadMessage.newInstance("msg", "data");
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        int messageMaxBuffer = 200;
        int messageCount = 300;
        PriorityMessageQueue queue = new PriorityMessageQueue(messageMaxBuffer,
                Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory()), new AlwaysBlockSender());
        AtomicInteger withinBuffer = new AtomicInteger();
        for (int i = 0; i < messageMaxBuffer + 1; ++i) {
            // +1 because the first "on flight" message is not counted in message buffer length
            // here we want to fill the buffer
            executor.execute(() -> {
                assertTimeoutPreemptively(Duration.of(500, ChronoUnit.MILLIS), () -> queue.sendMessageWithNormalPriority(msg));
                withinBuffer.incrementAndGet();
            });
        }
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> withinBuffer.get() > messageMaxBuffer);
        for (int i = 0; i < messageCount; ++i) {
            executor.execute(() -> assertThrows(AssertionError.class, () -> assertTimeoutPreemptively(Duration.of(500, ChronoUnit.MILLIS), () -> queue.sendMessageWithNormalPriority(msg))));
        }

        executor.close();
    }

    @Test
    void senderThrowsAnExceptionWhenQueueIsClosed() {
        GeneralPayloadMessage msg = GeneralPayloadMessage.newInstance("msg", "data");
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        int messageMaxBuffer = 200;
        int messageCount = 3000;
        PriorityMessageQueue queue = new PriorityMessageQueue(messageMaxBuffer,
                Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory()), new AlwaysBlockSender());
        AtomicBoolean closed = new AtomicBoolean(false);
        for (int i = 0; i < messageCount; ++i) {
            int finalI = i;
            executor.execute(() ->
            {
                boolean thrown = false;
                try {
                    queue.sendMessageWithNormalPriority(msg);
                } catch (Exception e) {
                    thrown = true;
                }
                Awaitility.await().until(closed::get);
                if (finalI > messageMaxBuffer) {
                    assertTrue(thrown);
                } else {
                    assertFalse(thrown);
                }
            });
        }
        queue.abort();
        executor.shutdown();
        for (int i = 0; i < 10; ++i) {
            assertThrows(Exception.class, () -> queue.sendMessageWithHighPriority(msg));
        }
    }

    @Test
    void keepAliveIsWorkingProperly() throws Exception {
        CachedMessageSender sender = new CachedMessageSender();
        PriorityMessageQueue queue = new PriorityMessageQueue(1,
                Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory()), sender);
        queue.enableKeepAlive(1000);
        for (int i = 1; i <= 3; ++i) {
            Thread.sleep(1200);
            assertEquals(i, sender.messages.size());
            assertInstanceOf(KeepAliveMessage.class, sender.messages.getLast());
        }
        assertThrows(Exception.class, () -> queue.enableKeepAlive(3000));
    }

    static class CachedMessageSender implements Function<Message, Void> {
        ArrayList<Message> messages = new ArrayList<>();

        @Override
        public Void apply(Message message) {
            messages.add(message);
            return null;
        }
    }

    static class AlwaysBlockSender implements Function<Message, Void> {
        @Override
        public Void apply(Message message) {
            for (; ; ) {

            }
        }
    }

}
