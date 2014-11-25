package com.chillenious.common.elasticsearch;

import com.chillenious.common.util.ArrayIterator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A page of results in a list.
 */
public class ListPage<T> implements Serializable, Iterable<T> {

    private int count;

    private int start;

    private int pageSize;

    private T[] objects;

    public ListPage() {
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ListPage<T> count(int count) {
        this.count = count;
        return this;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public ListPage<T> start(int start) {
        this.start = start;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public ListPage<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public T[] getObjects() {
        return objects;
    }

    public void setObjects(T[] objects) {
        this.objects = objects;
    }

    public ListPage<T> objects(T[] objects) {
        this.objects = objects;
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<>(objects);
    }

    public ListPage<T> copy(ListPage<?> p) {
        this.count = p.count;
        this.start = p.start;
        this.pageSize = p.pageSize;
        return this;
    }

    @Override
    public String toString() {
        return "ListPage{" +
                "count=" + count +
                ", start=" + start +
                ", pageSize=" + pageSize +
                ", objects=" + Arrays.toString(objects) +
                '}';
    }
}
