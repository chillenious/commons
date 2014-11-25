package com.chillenious.common.db.sync;

import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.util.Duration;

/**
 * Base class for classes that know how to refresh data from a store and
 * broadcast that to interested parties.
 *
 * @param <O> persistent object type
 */
public abstract class DataRefresher<O extends PersistentObject> {

    private final DataRefreshTopic<O> topic;

    private final DataRefreshListenerMarker<O> synchronizer;

    public DataRefresher(ShutdownHooks shutdownHooks) {
        this.topic = new DataRefreshTopic<>(shutdownHooks);
        this.synchronizer = new DataRefreshListenerMarker<>(topic);
    }

    /**
     * Trigger a refresh of the backing data store that is propagated to the
     * relevant dependent in-memory stores.
     *
     * @return summary of the refresh run
     */
    public abstract RefreshResults refresh();

    /**
     * Publishes event to interested parties.
     *
     * @param evt event to publish
     */
    protected final void publish(DataRefreshEvent<O> evt) {
        topic.publish(evt);
    }

    /**
     * Adds a listener.
     *
     * @param listener listener to add
     */
    public final void addListener(DataRefreshListener<O> listener) {
        listener.setTopic(topic);
        topic.addListener(listener);
    }

    /**
     * Removes a listener
     *
     * @param listener listener to remove
     */
    public final void removeListener(DataRefreshListener<O> listener) {
        topic.removeListener(listener);
        listener.setTopic(null);
    }

    /**
     * Calls {@link #refresh()} and then waits for listeners the finish processing
     * with the provided timeout.
     *
     * @param timeout maximum time to wait
     * @return refresh results
     */
    public final RefreshResults refreshAndWait(Duration timeout) {
        RefreshResults results = refresh();
        waitForListeners(timeout);
        return results;
    }

    /**
     * Block until all processing by listeners is done or the provided
     * timeout passes.
     *
     * @param timeout maximum time to wait
     */
    public final void waitForListeners(Duration timeout) {
        synchronizer.blockUntilNextMarker(timeout);
    }

    public void shutdown() {
        topic.shutdown();
    }
}
