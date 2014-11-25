package com.chillenious.common.db.sync;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Structure that works with a factory to produce sort keys and an ordered map
 * between these keys and persistent objects. It uses {@link java.util.concurrent.ConcurrentSkipListMap}
 * so that the impact of writes is minimal and iterating over the values in order
 * is fast.
 * <p/>
 * As part of the setup of this object, it should be {@link #bind(DataRefresher) bound}
 * to a refresher, and when it is taken out of commission, it should be
 * {@link #unbind(DataRefresher) unbound} again.
 *
 * @param <O> type of objects that are sorted
 */
public final class Sorter<O extends PersistentObject> {

    private final SortKeyFactory<O, ? extends Comparable> factory;

    private final NavigableMap<SortKey<? extends Comparable>, O> sorted =
            new ConcurrentSkipListMap<>();

    private final Map<Object, SortKey> reverseLookup = new HashMap<>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock r = rwl.readLock(), w = rwl.writeLock();

    SortDataRefreshListener<O> listener;

    Sorter(SortKeyFactory<O, ? extends Comparable> factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    O put(O object) {
        if (object == null || object.getId() == null) {
            throw new NullPointerException();
        }
        w.lock();
        try {
            SortKey oldKey = reverseLookup.get(object.getId());
            SortKey key = factory.create(object, (oldKey == null));
            if (oldKey != null && key != null) {
                reverseLookup.remove(object.getId());
                sorted.remove(oldKey);
            }
            if (key != null && (!SortKey.DELETE.equals(key))) {
                reverseLookup.put(object.getId(), key);
                Object previous = sorted.put(key, object);
                return (O)previous;
            } else {
                return null;
            }
        } finally {
            w.unlock();
        }
    }

    O remove(Object id) {
        if (id == null) {
            throw new NullPointerException();
        }
        w.lock();
        try {
            SortKey key = reverseLookup.remove(id);
            return (key != null) ? sorted.remove(key) : null;
        } finally {
            w.unlock();
        }
    }

    O get(Object id) {
        if (id == null) {
            throw new NullPointerException();
        }
        r.lock();
        try {
            SortKey key = reverseLookup.get(id);
            return (key != null) ? sorted.get(key) : null;
        } finally {
            r.unlock();
        }
    }

    public Collection<O> values() {
        return sorted.values();
    }

    public Set<Object> ids() {
        return reverseLookup.keySet();
    }

    public boolean contains(Object id) {
        return reverseLookup.containsKey(id);
    }

    synchronized void bind(DataRefresher<O> refresher) {
        if (listener == null) {
            listener = new SortDataRefreshListener<>(this);
            refresher.addListener(listener);
        } else {
            throw new IllegalStateException(
                    String.format("sorter %s already has an active listener (%s)", this, listener));
        }
    }

    synchronized void unbind(DataRefresher<O> refresher) {
        if (listener != null) {
            refresher.removeListener(listener);
        } else {
            throw new IllegalStateException(String.format("sorter %s wasn't bound", this));
        }
    }
}
