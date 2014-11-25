package com.chillenious.common.db.sync;

/**
 * Every {@link com.chillenious.common.db.sync.Sorter} gets a {@link com.chillenious.common.db.sync.DataRefreshListener} so that the index
 * can be updated (synchronously, without blocking the thread that triggered the update)
 *
 * @param <O> type of object to sort
 * @param <T> type of lookup/ key
 */
final class IndexDataRefreshListener<O extends PersistentObject, T>
        extends DataRefreshListener<O> {

    private final Indexer<O, T> indexer;

    IndexDataRefreshListener(Indexer<O, T> indexer) {
        this.indexer = indexer;
    }

    @Override
    protected void onEvent(DataRefreshEvent<O> evt) {
        if (evt instanceof DataCreatedEvent || evt instanceof DataChangedEvent) {
            O object = getObject(evt);
            indexer.put(object);
        } else if (evt instanceof DataDeletedEvent) {
            indexer.remove(evt.getId());
        } // else ignore
    }
}
