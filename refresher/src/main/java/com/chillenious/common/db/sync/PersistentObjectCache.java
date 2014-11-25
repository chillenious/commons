package com.chillenious.common.db.sync;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.WithShutdown;
import com.chillenious.common.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache that works with {@link com.chillenious.common.db.sync.PersistentObject persistent objects} (it is known that these
 * objects are backed by persistent storage), and supports a mechanism for getting fresh data
 * pushed to it.
 * <p/>
 * Typical caching mechanisms expire elements and load from secondary storage when clients
 * try to retrieve these elements, resulting in a slower lookup. For this case though, fast
 * reads are important, which is why instead this cache relies on it's contents to be refreshed
 * periodically with just minimal interference (limited locks) for reads.
 * <p/>
 * Guava caches provide a mechanism for asynchronously refreshing contents of the cache
 * (see {@link CacheLoader#reload(Object, Object)} and friends) that comes close to what we
 * need here. However, this cache wouldn't load new values, and the refresher mechanism we have
 * here is more generic, and hence can be used outside of caches (e.g. for ETL-like problems that
 * also need to update in-memory structures).
 * <p/>
 *
 * @param <O> type of the elements in the cache
 */
public class PersistentObjectCache<O extends PersistentObject>
        implements WithShutdown, PersistentObjectLookup<O> {

    private static final Logger log = LoggerFactory.getLogger(PersistentObjectCache.class);

    /**
     * Listens in on refreshes and updates the internal cache
     */
    final class RefreshListener extends DataRefreshListener<O> {

        @Override
        protected void onEvent(DataRefreshEvent<O> evt) {
            if (evt instanceof DataDeletedEvent) {
                Object id = evt.getId();
                cache.invalidate(id);
                if (cacheListener != null) {
                    cacheListener.afterDeleted(id);
                }
            } else if (evt instanceof DataCreatedEvent) {
                O object = ((DataCreatedEvent<O>) evt).getObject();
                cache.put(evt.getId(), object);
                if (cacheListener != null) {
                    cacheListener.afterPut(object);
                }
            } else if (evt instanceof DataChangedEvent) {
                O object = ((DataChangedEvent<O>) evt).getObject();
                cache.put(evt.getId(), object);
                if (cacheListener != null) {
                    cacheListener.afterPut(object);
                }
            } // else ignore; it's probably a special purpose event like req/ ack
        }
    }

    final class Refresher implements Runnable {
        @Override
        public void run() {
            try {
                log.debug(String.format("do refresh run for %s", name));
                try {
                    RefreshResults results = refresher.refresh();
                    if (results.getNumberRecordsFound() > 0) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("cache %s refreshed: %s", name, results));
                        }
                    }
                } catch (Exception e) {
                    log.error(String.format("problem with cache refresh for %s: %s",
                            name, e.getMessage()), e);
                }
                log.debug(String.format("exiting refresh daemon %s", this));
            } catch (Exception e) {
                log.error(String.format(
                        "problem with refresh run: %s; exiting",
                        e.getMessage()), e);
            }
        }
    }

    protected final String name; // mostly for debugging, monitoring

    protected final Cache<Object, O> cache;

    protected final DataRefresher<O> refresher;

    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    PersistentObjectCacheListener<O> cacheListener; // listener, for testing purposes

    protected final Map<String, Sorter<O>> sorters = new ConcurrentHashMap<>();

    protected final Map<String, Indexer<O, ?>> indexers = new ConcurrentHashMap<>();

    protected final ExecutorService seedSortsExecutor = Executors.newSingleThreadExecutor();

    /**
     * Construct. This instance will NOT use a refresher (or rather, it
     * will use a {@link com.chillenious.common.db.sync.NoopRefresher dummy} with no refreshes scheduled).
     *
     * @param name             name of the cache, mainly for debugging and monitoring
     * @param shutdownHooks    shutdown hook registry
     * @param recordStats      whether to record record stats (which will result
     *                         in a slight overhead of working with the cache, but
     *                         might give you interesting usage stats)
     * @param startWithRefresh whether to do a refresh when the instance is created (and hence
     *                         blocking the thread that is creating this instance, but also
     *                         guaranteeing that after creation, the cache will have a good
     *                         initial filling). If this is true, a refresh will be done
     *                         immediately, but the periodic refresh will be scheduled with an
     *                         initial delay that is equal to the delay between refreshes. If this is
     *                         false, construction will not block, but the refresh daemon will
     *                         be run immediately.
     */
    public PersistentObjectCache(
            String name,
            ShutdownHooks shutdownHooks,
            boolean recordStats,
            boolean startWithRefresh) {

        this(name, shutdownHooks, null, null, recordStats, startWithRefresh);
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
     * @param name             name of the cache, mainly for debugging and monitoring
     * @param shutdownHooks    shutdown hook registry
     * @param refresher        refresher this cache should use to keep
     *                         track of the backend; if null, a dummy will
     *                         be used and no refreshes will be scheduled
     * @param refreshInterval  interval for refreshes; if null, no refreshes will be
     *                         done aside from the initial refresh
     * @param recordStats      whether to record record stats (which will result
     *                         in a slight overhead of working with the cache, but
     *                         might give you interesting usage stats)
     * @param startWithRefresh whether to do a refresh when the instance is created (and hence
     *                         blocking the thread that is creating this instance, but also
     *                         guaranteeing that after creation, the cache will have a good
     *                         initial filling). If this is true, a refresh will be done
     *                         immediately, but the periodic refresh will be scheduled with an
     *                         initial delay that is equal to the delay between refreshes. If this is
     *                         false, construction will not block, but the refresh daemon will
     *                         be run immediately.
     */
    public PersistentObjectCache(
            String name,
            ShutdownHooks shutdownHooks,
            DataRefresher<O> refresher,
            Duration refreshInterval,
            boolean recordStats,
            boolean startWithRefresh) {

        this.name = Strings.isNullOrEmpty(name) ? this.toString() : name;
        log.info("creating cache " + this.name);
        if (recordStats) {
            this.cache = CacheBuilder.<Long, O>newBuilder().recordStats().build();
        } else {
            this.cache = CacheBuilder.<Long, O>newBuilder().build();
        }
        this.refresher = refresher != null ? refresher : new NoopRefresher<O>();
        shutdownHooks.add(this);
        initRefresher(refreshInterval, startWithRefresh);
    }

    /*
     * Initialize the refresher.
     */
    private void initRefresher(
            Duration refreshInterval,
            boolean startWithRefresh) {
        refresher.addListener(new RefreshListener()); // the listener will update the cache
        Duration refreshDaemonDelay;
        if (startWithRefresh) {
            refreshDaemonDelay = refreshInterval;
            log.info(String.format("start refresher for %s", name));
            RefreshResults results = refresher.refresh(); // load initial data
            log.info(String.format("initialized %s with %,d objects, in %,d milliseconds",
                    name, (results.getNumberCreated() + results.getNumberChanged()),
                    results.getMillisecondsItTook()));
        } else {
            refreshDaemonDelay = Duration.milliseconds(0);
        }
        if (refreshInterval != null) {
            scheduler.scheduleAtFixedRate(
                    new Refresher(),
                    refreshDaemonDelay.getMilliseconds(),
                    refreshInterval.getMilliseconds(),
                    TimeUnit.MILLISECONDS);
        }
        log.info(String.format("cache %s ready", name));
    }

    /**
     * Add a index for objects in this cache so that elements in the cache
     * can be looked up using a particular key. Adding an index will result
     * in the cache keeping track of the element in a dedicated data structure,
     * which increases the memory and load during writes.
     *
     * @param id      sort id
     * @param factory factory for creating lookup keys
     * @return future on the task of initial creation of the set; you can decide to
     * wait for that filling to be done in case you need the results right away,
     * or go on with execution if you don't. It returns the number of rows it sorted
     */
    public <T extends Comparable> Future<Integer> addIndex(
            final String id, IndexKeyFactory<O, T> factory) {

        if (id == null) {
            throw new NullPointerException();
        }
        if (factory == null) {
            throw new NullPointerException();
        }
        synchronized (indexers) {

            final Indexer<O, T> indexer = new Indexer<>(factory);
            Indexer<O, ?> previous = indexers.put(id, indexer);
            if (previous != null) {
                log.debug(String.format("replaced sort id %s for cache %s", id, name));
                previous.unbind(refresher);
            }
            // add listener that updates the sorted set when data changes
            indexer.bind(refresher);
            log.info(String.format("added index %s (key factory: %s) for cache %s", id, factory, name));
            // and finally, seed sort set in a different thread and return the future of the work
            return seedSortsExecutor.submit(new Callable<Integer>() {
                @Override
                public Integer call() {
                    try {
                        AtomicInteger count = new AtomicInteger(0);
                        for (O object : cache.asMap().values()) {
                            indexer.put(object);
                            count.incrementAndGet();
                        }
                        return count.get();
                    } catch (Exception e) {
                        String msg = String.format("problem adding index %s to cache %s: %s",
                                id, name, e.getMessage());
                        log.error(msg, e);
                        throw new IllegalStateException(e);
                    }
                }
            });
        }
    }

    /**
     * Gets indexer for the provided sort id.
     *
     * @param id id of the indexer
     * @return indexer or null if not found
     * @throws IllegalArgumentException when no indexer with the provided id was found
     */
    @SuppressWarnings("unchecked")
    protected <T> Indexer<O, T> getIndexer(String id) {
        if (id == null) {
            throw new NullPointerException();
        }
        Indexer indexer = indexers.get(id);
        if (indexer == null) {
            throw new IllegalArgumentException(String.format("no indexer found with id %s (cache %s)", id, name));
        }
        return indexer;
    }

    /**
     * Gets set of matches for indexed field based on the indexer (id) and the index value.
     *
     * @param indexId  id of the index
     * @param indexVal value of the index to fetch
     * @param <T>      type of the indev value
     * @return set of matches, possibly empty, never null
     */
    public <T> Set<O> getIndexed(String indexId, T indexVal) {
        return getIndexer(indexId).get(indexVal);
    }

    /**
     * Gets single matches for indexed field based on the indexer (id) and the index value.
     *
     * @param indexId  id of the index
     * @param indexVal value of the index to fetch
     * @param <T>      type of the indev value
     * @return single match, possibly null
     */
    @Nullable
    public <T> O getIndexedSingle(String indexId, T indexVal) {
        Set<O> s = getIndexer(indexId).get(indexVal);
        Iterator<O> iterator = s.iterator();
        if (iterator.hasNext()) {
            O object = iterator.next();
            if (iterator.hasNext()) {
                throw new IllegalStateException(
                        String.format("multiple matches for index %s, key %s (cache %s)",
                                indexId, indexVal, name)
                );
            }
            return object;
        }
        return null;
    }

    /**
     * Add a sort for objects in this cache so that elements in the cache
     * can be optimized for particular iteration. Adding a sort will result
     * in the cache keeping track of the element in a dedicated data structure,
     * which increases the memory and load during writes.
     *
     * @param id      sort id
     * @param factory factory for creating sort keys
     * @return future on the task of initial creation of the set; you can decide to
     * wait for that filling to be done in case you need the results right away,
     * or go on with execution if you don't. It returns the number of rows it sorted
     */
    public <T extends Comparable> Future<Integer> addSort(
            final String id, SortKeyFactory<O, T> factory) {

        if (id == null) {
            throw new NullPointerException();
        }
        if (factory == null) {
            throw new NullPointerException();
        }
        synchronized (sorters) {

            final Sorter<O> sorter = new Sorter<>(factory);
            Sorter<O> previous = sorters.put(id, sorter);
            if (previous != null) {
                log.info(String.format("replaced sort id %s for cache %s", id, name));
                previous.unbind(refresher);
            }
            // add listener that updates the sorted set when data changes
            sorter.bind(refresher);
            log.info(String.format("added sort %s (key factory: %s) for cache %s", id, factory, name));
            // and finally, seed sort set in a different thread and return the future of the work
            return seedSortsExecutor.submit(new Callable<Integer>() {
                @Override
                public Integer call() {
                    try {
                        AtomicInteger count = new AtomicInteger(0);
                        for (O object : cache.asMap().values()) {
                            sorter.put(object);
                            count.incrementAndGet();
                        }
                        return count.get();
                    } catch (Exception e) {
                        String msg = String.format("problem adding sort %s to cache %s: %s",
                                id, name, e.getMessage());
                        log.error(msg, e);
                        throw new IllegalStateException(e);
                    }
                }
            });
        }
    }

    /**
     * Gets sorter for the provided sort id.
     *
     * @param id id of the sort
     * @return sorter or null if not found
     * @throws IllegalArgumentException when no sorter with the provided id was found
     */
    protected Sorter<O> getSorter(String id) {
        if (id == null) {
            throw new NullPointerException();
        }
        Sorter<O> sorter = sorters.get(id);
        if (sorter == null) {
            throw new IllegalArgumentException(String.format(
                    "no sorter found with id %s in cache %s", id, name));
        }
        return sorter;
    }

    /**
     * Gets an iterator for the provided sort id.
     *
     * @param id id of the sort
     * @return iterator or null if not found
     */
    @Nullable
    public Collection<O> values(String id) {
        Sorter<O> sorter = getSorter(id);
        if (sorter != null) {
            return sorter.values();
        } else {
            return null;
        }
    }

    /**
     * Get unmodifiable map (id -> persistent object) over all values in the cache.
     *
     * @return map with ids and objects
     */
    public Map<Object, O> asMap() {
        return Collections.unmodifiableMap(cache.asMap());
    }

    /**
     * Get all values in the cache (in unspecified order... if you need order, create
     * a {@link #addSort(String, com.chillenious.common.db.sync.SortKeyFactory) sort}).
     *
     * @return collection of all values
     */
    public Collection<O> values() {
        return cache.asMap().values();
    }

    /**
     * Get set of all ids values in the cache (in unspecified order).
     *
     * @return set of all ids
     */
    public Set<Object> ids() {
        return cache.asMap().keySet();
    }

    /**
     * Returns the value associated with the provided id in this cache, or {@code null}
     * if there is no cached value for it.
     */
    @Nullable
    public O get(Object id) {
        return cache.getIfPresent(id);
    }

    /**
     * Put object in cache directly.
     * <p/>
     * WARNING: this method is meant primarily for testing, typical use is to rely
     * on the updater to work on the cache when new data comes in from the backend
     *
     * @param object object to put in the cache
     */
    public void put(O object) {
        synchronized (cache) {
            cache.put(object.getId(), object);
            for (Sorter<O> sorter : sorters.values()) {
                sorter.put(object);
            }
            for (Indexer<O, ?> indexer : indexers.values()) {
                indexer.put(object);
            }
        }
    }

    /**
     * Invalidate object with given key in cache directly.
     * WARNING: this method is meant primarily for testing, typical use is to rely
     * on the updater to work on the cache when new data comes in from the backend
     *
     * @param id id of the object to remove
     */
    public void remove(Object id) {
        synchronized (cache) {
            cache.invalidate(id);
            for (Sorter<O> sorter : sorters.values()) {
                sorter.remove(id);
            }
            for (Indexer<O, ?> indexer : indexers.values()) {
                indexer.remove(id);
            }
        }
    }

    /**
     * Gets {@link CacheStats stats} for this cache.
     *
     * @return gets cache stats.
     * @throws UnsupportedOperationException if the cache is not initialized to gather stats
     */
    public CacheStats stats() {
        return cache.stats();
    }

    /**
     * @return the approximate number of entries in this cache.
     */
    public long size() {
        return cache.size();
    }

    /**
     * Manually trigger refresh.
     *
     * @return refresh results
     */
    public RefreshResults refresh() {
        return refresher.refresh();
    }

    @SuppressWarnings("unchecked")
    @Override
    public O[] lookup(Object... ids) {
        Preconditions.checkNotNull(ids);
        O[] objects = (O[]) new Object[ids.length];
        for (int i = 0, l = ids.length; i < l; i++) {
            objects[i] = get(ids[i]);
        }
        return objects;
    }

    /**
     * Shut this cache down.
     */
    public void shutdown() {
        log.debug("shutting down cache " + name);
        scheduler.shutdownNow();
        seedSortsExecutor.shutdownNow();
        cache.invalidateAll();
    }
}
