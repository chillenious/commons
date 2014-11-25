package com.chillenious.common.db.sync;

/**
 * Event broadcast when previously existing data was changed (updates).
 *
 * @param <O> persistent object type
 */
public final class DataChangedEvent<O extends PersistentObject> extends DataRefreshEvent<O> {

    private final O object;

    public DataChangedEvent(O object) {
        super(object.getId());
        this.object = object;
    }

    public O getObject() {
        return object;
    }
}
