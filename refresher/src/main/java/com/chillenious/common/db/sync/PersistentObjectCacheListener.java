package com.chillenious.common.db.sync;

/**
 * Intercept cache puts and deletes. Really meant for testing only (which is why this class is package private).
 *
 * @param <O> type of cache
 */
interface PersistentObjectCacheListener<O> {

    /**
     * Called when an element is removed from the cache
     *
     * @param id id of the element
     */
    void afterDeleted(Object id);

    /**
     * Callend when an element is put (added or updated) in the cache
     *
     * @param object the object
     */
    void afterPut(O object);
}
