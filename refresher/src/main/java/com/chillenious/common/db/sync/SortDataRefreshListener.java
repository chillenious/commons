package com.chillenious.common.db.sync;

/**
 * Every {@link com.chillenious.common.db.sync.Sorter} gets a {@link com.chillenious.common.db.sync.DataRefreshListener} so that the sort
 * can be updated (synchronously, without blocking the thread that triggered the update)
 *
 * @param <O> type of object to sort
 */
final class SortDataRefreshListener<O extends PersistentObject>
        extends DataRefreshListener<O> {

    private final Sorter<O> sorter;

    SortDataRefreshListener(Sorter<O> sorter) {
        this.sorter = sorter;
    }

    @Override
    protected void onEvent(DataRefreshEvent<O> evt) {
        if (evt instanceof DataCreatedEvent || evt instanceof DataChangedEvent) {
            O object = getObject(evt);
            sorter.put(object);
        } else if (evt instanceof DataDeletedEvent) {
            sorter.remove(evt.getId());
        } // else ignore
    }
}
