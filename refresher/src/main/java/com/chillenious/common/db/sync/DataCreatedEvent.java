package com.chillenious.common.db.sync;

/**
 * Event broadcast when new data was found (inserts).
 *
 * @param <O> persistent object type
 */
public final class DataCreatedEvent<O extends PersistentObject> extends DataRefreshEvent<O> {

    private final O object;

    public DataCreatedEvent(O object) {
        super(object.getId());
        this.object = object;
    }

    public O getObject() {
        return object;
    }
}
