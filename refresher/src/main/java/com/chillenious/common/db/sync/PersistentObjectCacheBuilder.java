package com.chillenious.common.db.sync;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chillenious.common.Settings;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.util.Duration;

/**
 * Creates instances of {@link com.chillenious.common.db.sync.PersistentObjectCache}.
 * <p/>
 * If you use Guice to get an instance (recommended), you can use
 * setting {@code}caches.recordstats{@code} to configure whether
 * caches by default keep track of cache stats, and setting
 * <code>caches.startWithRefresh</code> to configure whether
 * by default caches do a refresh in immediate, blocking mode
 * or asynchronous with construction but non-blocking. Both of
 * these settings, when not provided, default to false.
 * <p/>
 * Use setting {@code}caches.startWithRefresh{@code} to configure whether
 * indexing should be ran at the start (true by default).
 */
@Singleton
public final class PersistentObjectCacheBuilder {

    private final ShutdownHooks shutdownHooks;

    private final boolean recordCacheStats;

    private final boolean startWithRefresh;

    @Inject
    public PersistentObjectCacheBuilder(
            ShutdownHooks shutdownHooks, Settings settings) {
        this.shutdownHooks = shutdownHooks;
        this.recordCacheStats = settings.getBoolean("caches.recordstats", false);
        this.startWithRefresh = settings.getBoolean("caches.startWithRefresh", false);
    }

    public static final class MruSortImpStep<O extends PersistentObject> extends BaseStep<O> {

        private final PersistentObjectCacheWithMRUSort.MruPromotionStrategy<O> strategy;

        public MruSortImpStep(String name,
                              ShutdownHooks shutdownHooks,
                              DataRefresher<O> refresher,
                              Duration refreshDuration,
                              boolean recordCacheStats,
                              boolean startWithRefresh) {
            this(name, shutdownHooks, refresher, refreshDuration,
                    recordCacheStats, startWithRefresh,
                    new PersistentObjectCacheWithMRUSort.PromoteWhenNewOnly<O>());
        }

        public MruSortImpStep(String name,
                              ShutdownHooks shutdownHooks,
                              DataRefresher<O> refresher,
                              Duration refreshDuration,
                              boolean recordCacheStats,
                              boolean startWithRefresh,
                              PersistentObjectCacheWithMRUSort.MruPromotionStrategy<O> strategy) {
            super(name, shutdownHooks, refresher, refreshDuration, recordCacheStats, startWithRefresh);
            this.strategy = strategy;
        }

        /**
         * Create a new {@link PersistentObjectCacheWithMRUSort persistent object cache}.
         *
         * @return new persistent object cache
         */
        public PersistentObjectCacheWithMRUSort<O> build() {
            return new PersistentObjectCacheWithMRUSort<>
                    (name, shutdownHooks, refresher, refreshDuration,
                            recordCacheStats, startWithRefresh, strategy);
        }

        /**
         * Only objects that are 'touched' or that are new to the cache are promoted
         * for the MRU sort.
         */
        public MruSortImpStep<O> promoteWhenNewOnly() {
            return new MruSortImpStep<>
                    (name, shutdownHooks, refresher, refreshDuration,
                            recordCacheStats, startWithRefresh,
                            new PersistentObjectCacheWithMRUSort.PromoteWhenNewOnly<O>());
        }

        /**
         * Objects that are 'touched', that are new to, or that are updated in the cache
         * are promoted for the MRU sort.
         */
        public MruSortImpStep<O> promoteWhenNewOrUpdated() {
            return new MruSortImpStep<>
                    (name, shutdownHooks, refresher, refreshDuration,
                            recordCacheStats, startWithRefresh,
                            new PersistentObjectCacheWithMRUSort.PromoteWhenNewOrUpdated<O>());
        }

        /**
         * Objects that are 'touched', that are new to, or that are updated in the cache
         * are promoted for the MRU sort.
         */
        public MruSortImpStep<O> promoteOnlyWhenTouched() {
            return new MruSortImpStep<>
                    (name, shutdownHooks, refresher, refreshDuration,
                            recordCacheStats, startWithRefresh,
                            new PersistentObjectCacheWithMRUSort.PromoteNever<O>());
        }

        /**
         * Use a custom MRU promotion strategy.
         *
         * @param strategy strategy for promotion the cache should use
         */
        public MruSortImpStep<O> withPromotionStrategy(
                PersistentObjectCacheWithMRUSort.MruPromotionStrategy<O> strategy) {
            return new MruSortImpStep<>(name, shutdownHooks, refresher, refreshDuration,
                    recordCacheStats, startWithRefresh, strategy);
        }
    }

    public static class BuildStep<O extends PersistentObject> extends BaseStep<O> {

        public BuildStep(String name,
                         ShutdownHooks shutdownHooks,
                         DataRefresher<O> refresher, Duration refreshDuration,
                         boolean recordCacheStats, boolean startWithRefresh) {
            super(name, shutdownHooks, refresher, refreshDuration,
                    recordCacheStats, startWithRefresh);
        }

        /**
         * Create a new {@link com.chillenious.common.db.sync.PersistentObjectCache persistent object cache}.
         *
         * @return new persistent object cache
         */
        public PersistentObjectCache<O> build() {
            return new PersistentObjectCache<>(
                    name, shutdownHooks, refresher, refreshDuration,
                    recordCacheStats, startWithRefresh);
        }

        /**
         * Enable cache stats for the instance, no matter what the settings say.
         *
         * @return build step
         */
        public BuildStep<O> withCacheStats() {
            return new BuildStep<>(name, shutdownHooks, refresher,
                    refreshDuration, true, startWithRefresh);
        }

        /**
         * Do not enable cache stats for the instance, no matter what the settings say.
         *
         * @return build step
         */
        public BuildStep<O> withoutCacheStats() {
            return new BuildStep<>(name, shutdownHooks, refresher,
                    refreshDuration, false, startWithRefresh);
        }

        /**
         * Do - no matter the settings - a refresh when the instance is created (and hence
         * blocking the thread that is creating the instance, but also guaranteeing that
         * after creation, the cache will have a good initial filling), and schedule any
         * periodic refresh with an initial delay that is equal to the delay between refreshes.
         *
         * @return build step
         */
        public BuildStep<O> withRefreshOnConstruction() {
            return new BuildStep<>(name, shutdownHooks, refresher,
                    refreshDuration, recordCacheStats, true);
        }

        /**
         * Do - no matter the settings - not do a refresh when the instance is created (and hence
         * the thread that is creating the instance won't be blocked, but the data is still loading
         * by the time the construction is done), and schedule any periodic refresh without an
         * initial delay.
         *
         * @return build step
         */
        public BuildStep<O> withoutRefreshOnConstruction() {
            return new BuildStep<>(name, shutdownHooks, refresher,
                    refreshDuration, recordCacheStats, false);
        }

        /**
         * Set the interval for refreshing from the backend (if not set explicitly,
         * the default of 20 minutes will be used).
         *
         * @param refreshDuration duration for refreshing from the backend
         * @return build step
         */
        public BuildStep<O> withRefreshEvery(Duration refreshDuration) {
            return new BuildStep<>(name, shutdownHooks, refresher,
                    refreshDuration, recordCacheStats, startWithRefresh);
        }

        /**
         * Use given name.
         *
         * @param name name of the cache that you can use for debugging and monitoring
         */
        public BuildStep<O> withName(String name) {
            return new BuildStep<>(name, shutdownHooks, refresher,
                    Duration.minutes(20), recordCacheStats, startWithRefresh);
        }

        /**
         * @return builder step that will produce a cache instance that can MRU sort
         */
        public MruSortImpStep<O> withMruSort() {
            return new MruSortImpStep<>(name, shutdownHooks, refresher,
                    refreshDuration, recordCacheStats, startWithRefresh);
        }
    }

    public static abstract class BaseStep<O extends PersistentObject> {

        final String name;

        final DataRefresher<O> refresher;

        final ShutdownHooks shutdownHooks;

        final boolean recordCacheStats;

        final Duration refreshDuration;

        final boolean startWithRefresh;

        public BaseStep(
                String name,
                ShutdownHooks shutdownHooks,
                DataRefresher<O> refresher, Duration refreshDuration,
                boolean recordCacheStats, boolean startWithRefresh) {
            this.name = name;
            this.shutdownHooks = shutdownHooks;
            this.recordCacheStats = recordCacheStats;
            this.refresher = refresher;
            this.refreshDuration = refreshDuration;
            this.startWithRefresh = startWithRefresh;
        }
    }

    /**
     * Start building a new instance.
     *
     * @return builder step
     */
    public <O extends PersistentObject> BuildStep<O> withRefresher(
            DataRefresher<O> refresher) {
        return new BuildStep<>(null, shutdownHooks, refresher,
                Duration.minutes(20), recordCacheStats, startWithRefresh);
    }
}
