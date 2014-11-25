package com.chillenious.common.db.sync;

import com.google.common.base.Objects;

/**
 * Base class that is recommended to extend from for {@link com.chillenious.common.db.sync.PersistentObject persistent objects}.
 */
public abstract class AbstractPersistentObject implements PersistentObject {

    private final Object id;

    protected AbstractPersistentObject(Object id) {
        this.id = id;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return (id == ((AbstractPersistentObject) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass(), id);
    }

    @Override
    public String toString() {
        return "AbstractPersistentObject{" +
                "id=" + id +
                '}';
    }
}
