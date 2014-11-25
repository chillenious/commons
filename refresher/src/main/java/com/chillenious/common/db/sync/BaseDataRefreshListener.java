package com.chillenious.common.db.sync;

/**
 * Base class for internal use. See {@link com.chillenious.common.db.sync.DataRefreshListener} for
 * the public version.
 *
 * @param <O> type of the persistent object
 */
abstract class BaseDataRefreshListener<O extends PersistentObject> {

    protected DataRefreshTopic<O> topic;

    /**
     * Called when new data was loaded/ found.
     *
     * @param evt event that encapsulates the find
     */
    public abstract void onDataRefresh(DataRefreshEvent<O> evt);

    void setTopic(DataRefreshTopic<O> topic) {
        this.topic = topic;
    }
}
