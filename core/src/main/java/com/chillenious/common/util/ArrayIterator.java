package com.chillenious.common.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ArrayIterator<T> implements Iterator<T>, Iterable<T> {

    private final T[] objects;

    private int ix;

    public ArrayIterator(T[] array) {
        objects = array;
        ix = 0;
    }

    @Override
    public boolean hasNext() {
        return objects != null && ix < objects.length;
    }

    @Override
    public T next() {
        if (ix >= objects.length) {
            throw new NoSuchElementException();
        }
        return objects[ix++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }
}
