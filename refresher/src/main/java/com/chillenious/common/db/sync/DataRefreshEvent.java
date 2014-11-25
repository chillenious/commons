package com.chillenious.common.db.sync;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Event broadcast when there is new data (a new or updated row)
 * for the {@link com.chillenious.common.db.sync.DataRefresher}.
 *
 * @param <O> persistent object type
 *
 * The reason we have this event hierarchy is
 */
public abstract class DataRefreshEvent<O extends PersistentObject> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long timestamp = System.currentTimeMillis();

    private final Object id;

    /**
     * Construct.
     *
     * @param id subject's id
     */
    protected DataRefreshEvent(Object id) {
        this.id = id;
    }

    /**
     * @return the id of the subject
     */
    public final Object getId() {
        return id;
    }

    /**
     * @return time in ms this event was created
     */
    public final long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass().equals(o.getClass())) {
            DataRefreshEvent that = (DataRefreshEvent) o;
            return Objects.equal(this.timestamp, that.timestamp) && Objects.equal(this.id, that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass(), this.timestamp, this.id);
    }

    @Override
    public String toString() {
        return "DataRefreshEvent{" +
                "timestamp=" + timestamp +
                ", id=" + id +
                '}';
    }
}
