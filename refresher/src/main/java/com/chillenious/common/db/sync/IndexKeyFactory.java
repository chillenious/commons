package com.chillenious.common.db.sync;

import javax.annotation.Nullable;

/**
 * Factory for index keys. Clients can create such factories and pass them in
 * {@link PersistentObjectCache#addIndex(String, com.chillenious.common.db.sync.IndexKeyFactory)} to keep the data
 * in the cache indexed in a particular fashion
 *
 * @param <O> type of the object it indexes
 * @param <T> type of the index keys
 */
@FunctionalInterface
public interface IndexKeyFactory<O extends PersistentObject, T> {

    /**
     * Create a new key for the passed in object.
     *
     * @param object the persistent object to create the key for
     * @return new key or null if this object shouldn't be added to the
     * index this works for
     */
    @Nullable
    T create(O object);
}
