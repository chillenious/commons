package com.chillenious.common.db.jooq.svc;

import com.chillenious.common.util.Wrapped;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;

/**
 * A page of results in a list.
 */
public final class ListPage<T> implements Serializable, Iterable<T>, Wrapped<ImmutableList<T>> {

    /**
     * Get list page instance using the provided offset, limit and objects (list).
     */
    public static <E> ListPage<E> of(int offset, int requestedLimit, E[] list) {
        return new ListPage<>(offset, requestedLimit, ImmutableList.copyOf(list));
    }

    /**
     * Get list page instance using the provided offset, limit and objects (list).
     */
    public static <E> ListPage<E> of(int offset, int requestedLimit, List<E> list) {
        return new ListPage<>(offset, requestedLimit, ImmutableList.copyOf(list));
    }

    private final int offset;

    private final int requestedLimit;

    private final ImmutableList<T> objects;

    private ListPage(int offset, int requestedLimit, ImmutableList<T> objects) {
        this.offset = offset;
        this.requestedLimit = requestedLimit;
        this.objects = Objects.requireNonNull(objects);
    }

    public int getOffset() {
        return offset;
    }

    public int getRequestedLimit() {
        return requestedLimit;
    }

    public ImmutableList<T> getObjects() {
        return objects;
    }

    @Override
    public UnmodifiableIterator<T> iterator() {
        return objects.iterator();
    }

    public int size() {
        return objects.size();
    }

    @Override
    public ImmutableList<T> getWrapped() {
        return objects;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("offset", offset)
                .add("requestedLimit", requestedLimit)
                .add("objects", objects)
                .toString();
    }

    /**
     * Collector that turns stream into a list page instance using the passed in limit object.
     *
     * @param limit limit that was used for the query
     */
    public static <T> Collector<T, List<T>, ListPage<T>> toListPage(Limit limit) {
        return toListPage(limit.getOffset(), limit.getNumberOfRows());
    }

    /**
     * Collector that turns stream into a list page instance using the passed in offset and number of rows.
     *
     * @param offset         results were fetched starting from...
     * @param requestedLimit requested maximum number of rows to be read after the offset (it is
     *                       possible that there were not that many rows from the offset, in which case the
     *                       objects in the page list are less than the requested limit)
     */
    public static <T> Collector<T, List<T>, ListPage<T>> toListPage(int offset, int requestedLimit) {
        return Collector.of(
                ArrayList::new,
                List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                (list) -> ListPage.of(offset, requestedLimit, list),
                Collector.Characteristics.CONCURRENT);
    }
}
