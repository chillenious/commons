package com.chillenious.common.db.sync;

/**
 * Abstracts lookup for persistent objects
 *
 * @param <O> type of persistent objects
 */
public interface PersistentObjectLookup<O extends PersistentObject> {

    /**
     * Lookup one or more objects by their ids.
     *
     * @param ids ids of the objects to lookup; may not be null
     * @return List of persistent objects that match the ids. Padding/ nulls
     * should be used for the 'slots' where no match was found for a provided id,
     * so for instance [{id=1, name=foo}, null, {id=3, name=bar}] should be
     * returned when objects for ids 1 and 3 exist, but 2 doesn't
     */
    O[] lookup(Object... ids);
}
