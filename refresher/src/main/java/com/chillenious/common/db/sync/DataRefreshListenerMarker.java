package com.chillenious.common.db.sync;

import com.chillenious.common.util.Duration;
import com.google.common.base.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility for testing that helps wait for messages sent up to a particular moment
 * to be processed by message event listeners. It's mostly meant for testing,
 * but it could be used in cases where it is important to synchronize the all listeners
 * of a refresh.
 */
final class DataRefreshListenerMarker<O extends PersistentObject>
        extends BaseDataRefreshListener<O> {

    private static final Logger log = LoggerFactory.getLogger(DataRefreshListenerMarker.class);

    private CountDownLatch countDownLatch = null;

    private MarkerReq markerReq;

    private final DataRefreshTopic<O> topic;

    DataRefreshListenerMarker(DataRefreshTopic<O> topic) {
        this.topic = topic;
    }

    /**
     * Send out an ack request and block execution of this thread until all handlers have sent the ack.
     *
     * @param timeout milliseconds to wait until giving up; null for no time out
     * @throws com.chillenious.common.db.sync.OverDueException if a timeOut is provided and the duration of it exceeds the waiting
     */
    @SuppressWarnings("unchecked")
    synchronized void blockUntilNextMarker(Duration timeout) {
        int listenersToWaitFor = topic.getNumberOfListeners();
        if (listenersToWaitFor > 0) {
            countDownLatch = new CountDownLatch(listenersToWaitFor);
            topic.addListener(DataRefreshListenerMarker.this);
            try {
                long startTs = System.currentTimeMillis();
                markerReq = new MarkerReq();
                topic.publish(markerReq);
                if (timeout != null) {
                    log.info(String.format("waiting for %d listeners to ACK (timeout=%s, topic = %s)",
                            listenersToWaitFor, timeout, topic));
                    if (!countDownLatch.await(timeout.getMilliseconds(), TimeUnit.MILLISECONDS)) {
                        throw new OverDueException(String.format(
                                "too much time (%s) elapsed while waiting for ACKs", timeout));
                    }
                } else {
                    log.info(String.format("waiting for %d listeners to ACK", listenersToWaitFor));
                    countDownLatch.await();
                }
                log.info(String.format("ACKs received after %,d milliseconds",
                        System.currentTimeMillis() - startTs));
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                Thread.currentThread().interrupt(); // restore interrupt status
            } finally {
                topic.removeListener(DataRefreshListenerMarker.this);
                countDownLatch = null;
            }
        } // else no-one to listen for, we can return right away
    }

    @Override
    public void onDataRefresh(DataRefreshEvent<O> evt) {
        if (evt instanceof MarkerAck && Objects.equal(evt.getId(), markerReq.getId())) {
            if (countDownLatch != null) {
                log.info("received ack");
                countDownLatch.countDown();
            } else {
                log.error("got an ACK message but we're not counting down");
            }
        }
    }

}
