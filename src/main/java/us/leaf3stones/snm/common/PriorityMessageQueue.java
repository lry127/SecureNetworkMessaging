package us.leaf3stones.snm.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.leaf3stones.snm.message.KeepAliveMessage;
import us.leaf3stones.snm.message.Message;
import us.leaf3stones.snm.message.NetIOException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class PriorityMessageQueue {
    private static final Logger logger = LoggerFactory.getLogger(PriorityMessageQueue.class);
    private final ExecutorService executor;
    private final int maxBufferedMessagesCount;
    private final Function<Message, ?> messagePusher;
    private final ArrayList<LinkedList<Message>> messageQueues = new ArrayList<>();
    private final Lock queueAccessLock = new ReentrantLock();
    private final Condition queueIsNotEmpty = queueAccessLock.newCondition();
    private final Condition queueIsNotFull = queueAccessLock.newCondition();
    private Future<?> keepTheQueueNotEmptyTask;
    private boolean active = true;
    private Exception fatalSendException;
    public PriorityMessageQueue(int maxBufferedMessagesCount, ExecutorService executor, Function<Message, ?> messagePusher) {
        this.maxBufferedMessagesCount = maxBufferedMessagesCount;
        this.messagePusher = messagePusher;
        for (int i = 0; i < Priority.QUEUE_SIZE; ++i) {
            messageQueues.add(new LinkedList<>());
        }
        executor.execute(new CopyMessageToPeerTask());
        this.executor = executor;
    }

    public void abort() {
        queueAccessLock.lock();
        try {
            active = false;
            // wait up all sending and copying thread
            // so that they have a change to exit
            queueIsNotFull.signalAll();
            queueIsNotEmpty.signalAll();
            if (keepTheQueueNotEmptyTask != null) {
                keepTheQueueNotEmptyTask.cancel(true);
            }
        } finally {
            queueAccessLock.unlock();
        }

    }

    public void enableKeepAlive(int intervalMillis) throws NetIOException {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        } else if (intervalMillis < 1000) {
            // at least 1 sec
            intervalMillis = 1000;
        }

        queueAccessLock.lock();
        if (!active) {
            queueAccessLock.unlock();
            throw getSenderException();
        }
        if (keepTheQueueNotEmptyTask != null) {
            queueAccessLock.unlock();
            throw new IllegalStateException("already keeping alive");
        }
        keepTheQueueNotEmptyTask = executor.submit(new KeepTheQueueNotEmptyTask(intervalMillis));
        queueAccessLock.unlock();
    }

    public void sendMessageWithRealtimePriority(Message message) throws NetIOException {
        sendMessage(message, Priority.REALTIME);
    }

    public void sendMessageWithHighPriority(Message message) throws NetIOException {
        sendMessage(message, Priority.HIGH);
    }

    public void sendMessageWithNormalPriority(Message message) throws NetIOException {
        sendMessage(message, Priority.NORMAL);
    }

    private void sendMessageWithIdlePriority(Message message) throws NetIOException {
        sendMessage(message, Priority.IDLE);
    }

    private NetIOException getSenderException() {
        if (active) {
            throw new IllegalStateException("--BUG-- trying to get exception when active");
        }
        if (fatalSendException == null) {
            return new NetIOException("no sender exception", false);
        }
        return new NetIOException(fatalSendException, true);
    }

    private void sendMessage(Message message, Priority priority) throws NetIOException {
        if (message == null) {
            throw new IllegalArgumentException("message can't be null");
        }
        if (priority == null) {
            priority = Priority.NORMAL;
        }

        queueAccessLock.lock();
        if (!active) {
            queueAccessLock.unlock();
            throw getSenderException();
        }
        // if queue is full, wait until some message is flushed unless it's a real time message
        while (priority != Priority.REALTIME && isQueueFull()) {
            try {
                queueIsNotFull.await();
                if (!active) {
                    queueAccessLock.unlock();
                    throw new IllegalStateException("output closed. no need to wait anymore");
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("we're interrupted while waiting to send a message");
            }
        }

        boolean signalNeeded = isQueueEmpty();
        messageQueues.get(priority.queuePos).add(message);
        if (signalNeeded) {
            queueIsNotEmpty.signal();
        }
        queueAccessLock.unlock();
    }

    private boolean isQueueEmpty() {
        for (List<Message> queue : messageQueues) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isQueueFull() {
        return messageQueues.stream().mapToInt(List::size).sum() >= maxBufferedMessagesCount;
    }

    private Message getNextSendingMessage() {
        for (List<Message> queue : messageQueues) {
            if (queue.isEmpty()) {
                continue;
            }
            return queue.removeFirst();
        }
        throw new RuntimeException("--BUG-- can't find available message");
    }

    private enum Priority {
        REALTIME(0),
        HIGH(1),
        NORMAL(2),
        IDLE(3);

        private static final int QUEUE_SIZE = Priority.values().length;
        private final int queuePos;

        Priority(int queuePos) {
            this.queuePos = queuePos;
        }
    }

    private class CopyMessageToPeerTask implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                queueAccessLock.lock();
                while (isQueueEmpty()) {
                    try {
                        queueIsNotEmpty.await();
                        if (!active) {
                            logger.info("output closed. no need to copy");
                            return;
                        }
                    } catch (InterruptedException e) {
                        logger.info("shutting down copy queue -- canceled");
                        abort();
                        return;
                    }
                }
                Message nextMsg = getNextSendingMessage();
                queueIsNotFull.signal();
                queueAccessLock.unlock();

                try {
                    messagePusher.apply(nextMsg);
                } catch (Exception e) {
                    logger.error("error while pushing message to peer: {}", e.getMessage());
                    fatalSendException = e;
                    abort();
                }
            }
        }
    }

    private class KeepTheQueueNotEmptyTask implements Runnable {
        private final int keepAliveIntervalMillis;

        public KeepTheQueueNotEmptyTask(int checkMillis) {
            keepAliveIntervalMillis = checkMillis;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(keepAliveIntervalMillis);
                } catch (InterruptedException interrupted) {
                    logger.debug("keep alive thread interrupted. return...");
                    return;
                }

                queueAccessLock.lock();
                try {
                    if (isIdleQueueEmpty()) {
                        sendMessageWithIdlePriority(KeepAliveMessage.newInstance());
                    }
                } catch (Exception e) {
                    logger.warn("failed to send idle message. stop keeping alive. error: {}", e.getMessage());
                    return;
                } finally {
                    queueAccessLock.unlock();
                }
            }
        }

        private boolean isIdleQueueEmpty() {
            return messageQueues.get(Priority.IDLE.queuePos).isEmpty();
        }
    }
}