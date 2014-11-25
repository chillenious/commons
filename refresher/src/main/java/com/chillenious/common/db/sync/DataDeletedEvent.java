package com.chillenious.common.db.sync;

/**
 * Event broadcast when previously existing data was deleted.
 *
 * @param <O> persistent object type
 */
public final class DataDeletedEvent<O extends PersistentObject> extends DataRefreshEvent<O> {

    public DataDeletedEvent(long id) {
        super(id);
    }
}
