package com.chillenious.common.db.sync;

import java.io.Serializable;

/**
 * Base class for objects that are backed by data in a persistent store and that
 * are able to work with the caches and refreshers from this package.
 */
public interface PersistentObject extends Serializable {

    /**
     * @return the persistent object's id
     */
    public abstract Object getId();
}
