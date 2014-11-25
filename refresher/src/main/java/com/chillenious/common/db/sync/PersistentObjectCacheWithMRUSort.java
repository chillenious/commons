package com.chillenious.common.db.sync;

import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.util.CurrentTime;
import com.chillenious.common.util.Duration;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Persistent object cache that keeps track of elements in order of most recently used
 * (so the most recently used element is on the front when iterating over the set, while
 * the last recently used would be the last element).
 *
 * @param <O> type of the elements in the cache
 */
public class PersistentObjectCacheWithMRUSort<O extends PersistentObject>
        extends PersistentObjectCache<O> {

    public static final String MRU_SORT_ID = "mru";

    /**
     * available options for promotion strategy answers.
     */
    public static enum PromoteOption {
        PROMOTE, IGNORE, DELETE
    }

    /**
     * Strategy that allows you to influence when
     *
     * @param <O> object type
     */
    public static interface MruPromotionStrategy<O extends PersistentObject> {

        /**
         * Whether to promote the object to the front of the MRU sort.
         *
         * @param object object to potentially promote
         * @param isNew  whether the object is already in the set
         * @return whether to promote the object
         */
        PromoteOption promote(O object, boolean isNew);
    }

    /**
     * Default strategy that promotes objects to the front of the MRU sorted set
     * when they are new to it. Updates won't effect it.
     * Aside from updating them when {@link #touch(com.chillenious.common.db.sync.PersistentObject)} is called, which always
     * results in a promotion.
     *
     * @param <O> object type
     */
    public static final class PromoteWhenNewOnly<O extends PersistentObject>
            implements MruPromotionStrategy<O> {
        @Override
        public PromoteOption promote(O object, boolean isNew) {
            return isNew ? PromoteOption.PROMOTE : PromoteOption.IGNORE;
        }
    }

    /**
     * Strategy that promotes objects to the front of the MRU sorted set
     * when they are new or updated (i.e. always).
     * Aside from updating them when {@link #touch(com.chillenious.common.db.sync.PersistentObject)} is called, which always
     * results in a promotion.
     *
     * @param <O> object type
     */
    public static final class PromoteWhenNewOrUpdated<O extends PersistentObject>
            implements MruPromotionStrategy<O> {
        @Override
        public PromoteOption promote(O object, boolean isNew) {
            return PromoteOption.PROMOTE;
        }
    }

    /**
     * Strategy that never promotes objects to the front of the MRU sorted set
     * unless {@link #touch(com.chillenious.common.db.sync.PersistentObject)} is called, which always
     * results in a promotion.
     *
     * @param <O> object type
     */
    public static final class PromoteNever<O extends PersistentObject>
            implements MruPromotionStrategy<O> {
        @Override
        public PromoteOption promote(O object, boolean isNew) {
            return PromoteOption.IGNORE;
        }
    }

    private final Sorter<O> mruSorter;

    /**
     * Construct. This instance will NOT use a refresher (or rather, it
     * will use a {@link com.chillenious.common.db.sync.NoopRefresher dummy} with no refreshes scheduled).
     *
     * @param name                 name of the cache, mainly for debugging and monitoring
     * @param shutdownHooks        shutdown hook registry
     * @param recordStats          whether to record record stats (which will result
     *                             in a slight overhead of working with the cache, but
     *                             might give you interesting usage stats)
     * @param mruPromotionStrategy strategy to use when deciding to promote
     *                             an element to the front of the MRU sort
     * @param startWithRefresh     whether to do a refresh when the instance is created (and hence
     *                             blocking the thread that is creating this instance, but also
     *                             guaranteeing that after creation, the cache will have a good
     *                             initial filling). If this is true, a refresh will be done
     *                             immediately, but the periodic refresh will be scheduled with an
     *                             initial delay that is equal to the delay between refreshes. If this is
     *                             false, construction will not block, but the refresh daemon will
     *                             be run immediately.
     */
    public PersistentObjectCacheWithMRUSort(
            String name,
            ShutdownHooks shutdownHooks,
            boolean recordStats,
            boolean startWithRefresh,
            final MruPromotionStrategy<O> mruPromotionStrategy) {
        this(name, shutdownHooks, null, null, recordStats, startWithRefresh, mruPromotionStrategy);
    }

    /**
     * Construct. Some arguments are optional:
     * <ul>
     * <li>refresher - if this is null, a {@link com.chillenious.common.db.sync.NoopRefresher dummy} will
     * be used and no refreshes will be scheduled</li>
     * <li>refreshInterval - is this is null, no refreshes will be
     * scheduled (but an initial refresh will be done)</li>
     * </ul>
     *
     * @param name                 name of the cache, mainly for debugging and monitoring
     * @param shutdownHooks        shutdown hook registry
     * @param refresher            refresher this cache should use to keep
     *                             track of the backend; if null, a dummy will
     *                             be used and no refreshes will be scheduled
     * @param refreshInterval      interval for refreshes; if null, no refreshes will be
     *                             done aside from the initial refresh
     * @param recordStats          whether to record record stats (which will result
     *                             in a slight overhead of working with the cache, but
     *                             might give you interesting usage stats)
     * @param mruPromotionStrategy strategy to use when deciding to promote
     *                             an element to the front of the MRU sort
     * @param startWithRefresh     whether to do a refresh when the instance is created (and hence
     *                             blocking the thread that is creating this instance, but also
     *                             guaranteeing that after creation, the cache will have a good
     *                             initial filling). If this is true, a refresh will be done
     *                             immediately, but the periodic refresh will be scheduled with an
     *                             initial delay that is equal to the delay between refreshes. If this is
     *                             false, construction will not block, but the refresh daemon will
     *                             be run immediately.
     */
    public PersistentObjectCacheWithMRUSort(
            String name,
            ShutdownHooks shutdownHooks,
            DataRefresher<O> refresher,
            Duration refreshInterval,
            boolean recordStats,
            boolean startWithRefresh,
            final MruPromotionStrategy<O> mruPromotionStrategy) {

        super(name, shutdownHooks, refresher, refreshInterval, recordStats, startWithRefresh);
        try {
            super.addSort(MRU_SORT_ID, (object, isNew) -> {
                PromoteOption option = mruPromotionStrategy.promote(object, isNew);
                if (PromoteOption.PROMOTE.equals(option)) {
                    DescendingLong lastAccessed = new DescendingLong(
                            CurrentTime.currentTimeMillis());
                    return new SortKey<>(object.getId(), lastAccessed);
                } else if (PromoteOption.IGNORE.equals(option)) {
                    return null; // should result in the update being ignored
                } else if (PromoteOption.DELETE.equals(option)) {
                    return SortKey.DELETE;
                } else {
                    throw new IllegalStateException("unknown response " + option);
                }
            }).get();

            this.mruSorter = getSorter(MRU_SORT_ID);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T extends Comparable> Future<Integer> addSort(
            String id, SortKeyFactory<O, T> factory) {

        if (Objects.equal(id, MRU_SORT_ID)) {
            throw new IllegalArgumentException(String.format("sort id %s is reserved", MRU_SORT_ID));
        }
        return super.addSort(id, factory);
    }

    /**
     * Update the sorted set of O by 'touching' it, which should put it in front
     * of the other elements.
     *
     * @param object object to touch
     */
    public void touch(O object) {
        O previous = mruSorter.remove(object.getId()); // updates might be ignored, so remove manually
        mruSorter.put(object);
    }

    /**
     * @return the elements in this case in most recently used order
     */
    public Collection<O> valuesByMostRecentlyUsed() {
        return mruSorter.values();
    }

    /**
     * Short cut to get direct handle on sorter
     *
     * @return sorter
     */
    public Sorter<O> getMruSorter() {
        return mruSorter;
    }
}
