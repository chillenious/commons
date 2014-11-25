package com.chillenious.common.elasticsearch;

import com.chillenious.common.Settings;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.db.sync.DataRefresher;
import com.chillenious.common.db.sync.PersistentObject;
import com.chillenious.common.db.sync.PersistentObjectLookup;
import com.chillenious.common.util.Duration;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.client.Client;

/**
 * Creates instances of {@link com.chillenious.common.elasticsearch.SearchIndexer}.
 * <p>
 * Use setting {@code}fulltextindexers.startWithRefresh{@code} to configure whether
 * indexing should be ran at the start (true by default).
 */
@Singleton
public final class SearchIndexerBuilder {

    private final ShutdownHooks shutdownHooks;

    private final boolean startWithRefresh;

    private final Client searchClient;

    @Inject
    public SearchIndexerBuilder(
            ShutdownHooks shutdownHooks, Settings settings,
            Client searchClient) {
        this.shutdownHooks = shutdownHooks;
        this.startWithRefresh = settings.getBoolean("fulltextindexers.startWithRefresh", true);
        this.searchClient = searchClient;
    }

    public static class BuildStep<O extends PersistentObject> extends BaseStep<O> {

        public BuildStep(Class<O> type,
                         IndexAddressing addressing,
                         ShutdownHooks shutdownHooks,
                         DataRefresher<O> refresher,
                         PersistentObjectLookup<O> lookup,
                         Duration refreshDuration,
                         boolean startWithRefresh,
                         Client searchClient) {
            super(type, addressing, shutdownHooks, refresher, lookup,
                    refreshDuration, startWithRefresh, searchClient);
        }

        /**
         * Create a new {@link com.chillenious.common.db.sync.PersistentObjectCache persistent object cache}.
         *
         * @return new persistent object cache
         */
        public SearchIndexer<O> build() {
            return new SearchIndexer<>(
                    type, addressing, shutdownHooks, refresher, lookup,
                    refreshDuration, startWithRefresh, searchClient);
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
            return new BuildStep<>(type, addressing, shutdownHooks, refresher,
                    lookup, refreshDuration, true, searchClient);
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
            return new BuildStep<>(type, addressing, shutdownHooks, refresher,
                    lookup, refreshDuration, false, searchClient);
        }

        /**
         * Set the interval for refreshing from the backend (if not set explicitly,
         * the default of 20 minutes will be used).
         *
         * @param refreshDuration duration for refreshing from the backend
         * @return build step
         */
        public BuildStep<O> withRefreshEvery(Duration refreshDuration) {
            return new BuildStep<>(type, addressing, shutdownHooks, refresher,
                    lookup, refreshDuration, startWithRefresh, searchClient);
        }
    }

    public static abstract class BaseStep<O extends PersistentObject> {

        final Class<O> type;

        final IndexAddressing addressing;

        final DataRefresher<O> refresher;

        final PersistentObjectLookup<O> lookup;

        final ShutdownHooks shutdownHooks;

        final Duration refreshDuration;

        final boolean startWithRefresh;

        final Client searchClient;

        public BaseStep(Class<O> type,
                        IndexAddressing addressing,
                        ShutdownHooks shutdownHooks,
                        DataRefresher<O> refresher,
                        PersistentObjectLookup<O> lookup,
                        Duration refreshDuration,
                        boolean startWithRefresh,
                        Client searchClient) {
            this.type = type;
            this.addressing = addressing;
            this.shutdownHooks = shutdownHooks;
            this.refresher = refresher;
            this.lookup = lookup;
            this.refreshDuration = refreshDuration;
            this.startWithRefresh = startWithRefresh;
            this.searchClient = searchClient;
        }
    }

    /**
     * Start building a new instance.
     *
     * @return builder step
     */
    public <O extends PersistentObject> BuildStep<O> with(
            Class<O> type,
            IndexAddressing addressing,
            DataRefresher<O> refresher,
            PersistentObjectLookup<O> lookup) {
        Preconditions.checkNotNull(refresher, lookup);
        return new BuildStep<>(type, addressing, shutdownHooks, refresher,
                lookup, Duration.minutes(2), startWithRefresh, searchClient);
    }
}
