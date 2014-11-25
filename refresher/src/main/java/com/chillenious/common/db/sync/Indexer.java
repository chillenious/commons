package com.chillenious.common.db.sync;

import com.google.common.collect.HashMultimap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Structure that works with a factory to produce index keys and a map
 * between these keys and persistent objects. It uses {@link java.util.concurrent.ConcurrentHashMap}
 * so that the impact of writes is minimal and access should generally fast
 *
 * @param <O> type of objects that are sorted
 * @param <T> key type
 */
public final class Indexer<O extends PersistentObject, T> {

    private final IndexKeyFactory<O, T> factory;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock r = rwl.readLock(), w = rwl.writeLock();

    private final HashMultimap<T, O> indexed = HashMultimap.create();

    private final Map<Object, T> reverseLookup = new HashMap<>();

    IndexDataRefreshListener<O, T> listener;

    Indexer(IndexKeyFactory<O, T> factory) {
        this.factory = factory;
    }

    void put(O object) {
        if (object == null || object.getId() == null) {
            throw new NullPointerException();
        }
        T key = factory.create(object);
        if (key != null) {
            w.lock();
            try {
                T previous = reverseLookup.remove(object.getId());
                if (previous != null) {
                    indexed.remove(previous, object);
                }
                indexed.put(key, object);
                reverseLookup.put(object.getId(), key);
            } finally {
                w.unlock();
            }
        }
    }

    void remove(Object id) {
        if (id == null) {
            throw new NullPointerException();
        }
        w.lock();
        try {
            T key = reverseLookup.remove(id);
            if (key != null) {
                for (Iterator<O> i = indexed.get(key).iterator(); i.hasNext(); ) {
                    O object = i.next();
                    if (object.getId() == id) {
                        i.remove();
                    }
                }
            } // object wasn't in here to start with
        } finally {
            w.unlock();
        }
    }

    Set<O> get(T key) {
        if (key == null) {
            throw new NullPointerException();
        }
        r.lock();
        try {
            return indexed.get(key);
        } finally {
            r.unlock();
        }
    }

    synchronized void bind(DataRefresher<O> refresher) {
        if (listener == null) {
            listener = new IndexDataRefreshListener<>(this);
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
