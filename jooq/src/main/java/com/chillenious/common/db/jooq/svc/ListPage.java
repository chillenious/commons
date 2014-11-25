package com.chillenious.common.db.jooq.svc;

import com.chillenious.common.util.Wrapped;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
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
     * Implements a ListPageCollector as an ArrayList with a finisher. Probably a naive implementation to start.
     * @param <T>
     */
    public static class ListPageCollector<T> implements Collector<T, List<T>, ListPage<T>> {
        private final Limit limit;

        public ListPageCollector(Limit limit) {
            this.limit = limit;
        }

        @Override
        public Supplier<List<T>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<T>, T> accumulator() {
            return (list, t) -> list.add(t);
        }

        @Override
        public BinaryOperator<List<T>> combiner() {
            return (left, right) -> {
                left.addAll(right);
                return left;
            };
        }

        @Override
        public Function<List<T>, ListPage<T>> finisher() {
            return (list) -> ListPage.of(limit.getOffset(), limit.getNumberOfRows(), list);
        }

        @Override
        public Set<Characteristics> characteristics() {
            return null;
        }
    }

    public static <T> ListPageCollector<T> toListPage(Limit limit) {
        return new ListPageCollector<>(limit);
    }
}
