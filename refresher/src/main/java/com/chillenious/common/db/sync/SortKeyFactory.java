package com.chillenious.common.db.sync;

import javax.annotation.Nullable;

/**
 * Factory for sort keys. Clients can create such factories and pass them in
 * {@link PersistentObjectCache#addSort(String, com.chillenious.common.db.sync.SortKeyFactory)} to keep the data
 * in the cache sorted according to the order that T provides.
 *
 * @param <T> type of the sort
 * @param <O> type of the object it sorts
 */
@FunctionalInterface
public interface SortKeyFactory<O extends PersistentObject, T extends Comparable> {

    /**
     * Create a new key for the passed in object. When this returns null, no
     * element will be put in the sorted set.
     *
     * @param object the persistent object to create the key for
     * @param isNew  whether the object is already in the sorted set
     * @return new key or null
     */
    @Nullable
    SortKey<T> create(O object, boolean isNew);
}
