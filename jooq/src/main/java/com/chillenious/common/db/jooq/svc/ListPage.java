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
 * A page of results in a list. Works together with {@link com.chillenious.common.db.jooq.svc.Limit}.
 * This is to wrap results of a pageable query that was requested with a Limit object.
 */
public class ListPage<T> implements Serializable, Iterable<T>, Wrapped<ImmutableList<T>> {

    private static final long serialVersionUID = 1L;

    /**
     * Get list page instance using the provided limit and objects.
     */
    public static <E> ListPage<E> of(Limit limit, List<E> list) {
        return new ListPage<>(limit, ImmutableList.copyOf(list));
    }

    private final Limit limit;

    private final ImmutableList<T> objects;

    // optional field (will be null if not set) that tells whether there are more rows
    // beyond this page at the time the result is constructed
    private final Boolean hasMore;

    protected ListPage(Limit limit, ImmutableList<T> objects) {
        Objects.requireNonNull(objects);
        this.limit = Objects.requireNonNull(limit);
        if (limit.getTestForMore()) {
            int max = limit.getNumberOfRows();
            if (objects.size() > max) {
                hasMore = true;
                List<T> partialCopy = new ArrayList<>(limit.getNumberOfRows());
                for (int i = 0; i < max; i++) {
                    partialCopy.add(objects.get(i));
                }
                this.objects = ImmutableList.copyOf(partialCopy);
            } else {
                hasMore = false;
                this.objects = objects;
            }
        } else {
            hasMore = null;
            this.objects = objects;
        }
    }

    public int getOffset() {
        return limit.getOffset();
    }

    public int getRequestedLimit() {
        return limit.getNumberOfRows();
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

    /**
     * Whether there are more records beyond the results in this list page. This is
     * null if the {@link com.chillenious.common.db.jooq.svc.Limit limit} wasn't
     * requested to include this info.
     */
    public Boolean getHasMore() {
        return hasMore;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("limit", limit)
                .add("objects", objects)
                .add("hasMore", hasMore)
                .toString();
    }

    /**
     * Collector that turns stream into a list page instance using the passed in limit object.
     *
     * @param limit limit that was used for the query
     */
    public static <T> Collector<T, List<T>, ListPage<T>> toListPage(Limit limit) {
        return Collector.of(
                ArrayList::new,
                List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                (list) -> ListPage.of(limit, list),
                Collector.Characteristics.CONCURRENT);
    }
}
