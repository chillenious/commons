package com.chillenious.common.db.sync;

import com.google.inject.Inject;
import com.chillenious.common.ShutdownHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Topic that sends out events asynchronously amongst the registered listeners, but
 * synchronously, FIFO for each listener, all locally (in the VM).
 *
 * @param <O> persistent object type
 */
final class DataRefreshTopic<O extends PersistentObject> {

    private static final Logger log = LoggerFactory.getLogger(DataRefreshTopic.class);

    private final Map<BaseDataRefreshListener<O>, ListenerQueue<O>> listeners =
            new LinkedHashMap<>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock r = rwl.readLock(), w = rwl.writeLock();

    /**
     * each listener gets it's own queue in it's own thread. After creation,
     * {@link #start()} needs to be called to actually poll for events.
     */
    static final class ListenerQueue<O extends PersistentObject> {

        private final BlockingQueue<DataRefreshEvent<O>> queue = new LinkedBlockingQueue<>();

        private final BaseDataRefreshListener<O> listener;

        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        final class PollDaemon implements Runnable {
            @Override
            public void run() {
                log.debug("start polling data refresh event queue for " + listener);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        DataRefreshEvent<O> evt = queue.take();
                        try {
                            listener.onDataRefresh(evt);
                        } catch (Exception e) {
                            log.error(String.format(
                                    "problem handling data refresh event by listener %s: %s%n\tevent: %s",
                                    listener, e.getMessage(), evt), e);
                        }
                    } catch (InterruptedException e) {
                        //log.error(e.getMessage(), e);
                        Thread.currentThread().interrupt(); // restore interrupt status and let exit
                    }
                }
                log.debug(String.format(
                        "exiting commit event queue polling for %s with %,d " +
                                "events in the queue left",
                        listener, queue.size()));
            }
        }

        ListenerQueue(BaseDataRefreshListener<O> listener) {
            if (listener == null) {
                throw new NullPointerException();
            }
            this.listener = listener;
            start();
        }

        void offer(DataRefreshEvent<O> evt) {
            if (!queue.offer(evt)) {
                throw new IllegalStateException(String.format(
                        "unable to add event to queue for listener %s", listener));
            }
        }

        void start() {
            executor.execute(new PollDaemon());
        }

        void shutdown() {
            executor.shutdownNow();
            log.debug("queue for " + listener + " shut down");
        }
    }

    @Inject
    DataRefreshTopic(ShutdownHooks shutdownHooks) {
        shutdownHooks.add(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    /**
     * Publish data commit event to the registered listeners.
     *
     * @param evt event to publish
     */
    void publish(DataRefreshEvent<O> evt) {
        r.lock();
        try {
            for (Map.Entry<BaseDataRefreshListener<O>, ListenerQueue<O>> entry : listeners.entrySet()) {
                try {
                    entry.getValue().offer(evt);
                } catch (Exception e) {
                    log.error(String.format(
                            "problem publishing event %s to queue %s for listener %s",
                            evt, entry.getValue(), entry.getKey()), e);
                }
            }
        } finally {
            r.unlock();
        }
    }

    /**
     * Adds a listener; will be called in order it was added compared to other listeners.
     *
     * @param listener listener
     */
    void addListener(BaseDataRefreshListener<O> listener) {
        w.lock();
        try {
            listeners.put(listener, new ListenerQueue<O>(listener));
            listener.setTopic(this);
        } finally {
            w.unlock();
        }
    }

    /**
     * Removes the provided listener.
     *
     * @param listener listener to remove
     * @throws IllegalArgumentException if the listener wasn't registered with this topic
     */
    void removeListener(BaseDataRefreshListener<O> listener) {
        ListenerQueue<O> q;
        w.lock();
        try {
            q = listeners.remove(listener);
        } finally {
            w.unlock();
        }
        if (q == null) {
            throw new IllegalArgumentException(
                    String.format("listener %s does not seem to be registered", listener));
        }
        q.shutdown();
    }

    /**
     * @return the number of listeners registered with the topic
     */
    int getNumberOfListeners() {
        r.lock();
        try {
            return listeners.size();
        } finally {
            r.unlock();
        }
    }

    void shutdown() {
        w.lock();
        try {
            for (ListenerQueue<O> listenerQueue : listeners.values()) {
                try {
                    listenerQueue.shutdown();
                } catch (Exception e) {
                    log.error("problem shutting down listener queue" + listenerQueue);
                }
            }
        } finally {
            w.unlock();
        }
    }

    @Override
    public String toString() {
        return "DataRefreshTopic{" +
                "listeners=" + listeners +
                '}';
    }
}
