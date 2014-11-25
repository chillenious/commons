package com.chillenious.common.db.sync;

import com.google.common.base.Objects;

/**
 * Key that is used to sort a persistent object in a particular fashion.
 *
 * @param <T> type of the sort object - sort relies on the order the comparable object provides.
 */
public class SortKey<T extends Comparable> implements Comparable<SortKey<T>> {

    /**
     * Special key if you want an existing element REMOVED from the cache.
     */
    public static final SortKey DELETE = new SortKey(null, null) {

        @Override
        public String toString() {
            return "[DELETE]";
        }
    };

    private final Object id;

    private final Comparable sort;

    public static class SortKeyBuildStep {
        private final Object id;

        public SortKeyBuildStep(Object id) {
            this.id = id;
        }

        public <T extends Comparable> SortKey<T> create(T key) {
            return new SortKey<>(id, key);
        }
    }

    public static SortKeyBuildStep forObject(PersistentObject object) {
        return new SortKeyBuildStep(object.getId());
    }

    public static SortKeyBuildStep forId(long id) {
        return new SortKeyBuildStep(id);
    }

    /**
     * Construct.
     *
     * @param object the persistent object
     * @param sort   sort
     */
    SortKey(PersistentObject object, Comparable sort) {
        this.id = object != null ? object.getId() : null;
        this.sort = sort;
    }

    /**
     * Construct.
     *
     * @param id   id of the persistent object
     * @param sort sort
     */
    SortKey(Object id, Comparable sort) {
        this.id = id;
        this.sort = sort;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(SortKey<T> o) {
        if (sort != null) {
            return sort.compareTo(o.sort);
        } else if (o == null) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SortKey) {
            SortKey that = (SortKey) o;
            return Objects.equal(this.id, that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(SortKey.class, id);
    }

    @Override
    public String toString() {
        return "[id=" + id +
                "/ sort=" + sort +
                ']';
    }
}
