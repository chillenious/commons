package com.chillenious.common.caching;

import com.chillenious.common.util.Duration;
import com.chillenious.common.util.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Singleton;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Cache of caches for method invocation results.
 */
@Singleton
public class MethodResultsCaches implements Iterable<Cache<String, Object>> {

    private final LoadingCache<CacheReturnValue, Cache<String, Object>> cacheCache;

    MethodResultsCaches() { // protected, because should be injected
        this.cacheCache = CacheBuilder.newBuilder().build(
                new CacheLoader<CacheReturnValue, Cache<String, Object>>() {
                    @Override
                    public Cache<String, Object> load(CacheReturnValue key) throws Exception {

                        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
                        if (!Strings.isEmpty(key.expireAfterWrite())) {
                            Duration expireAfterWrite = Duration.valueOf(key.expireAfterWrite());
                            builder.expireAfterWrite(expireAfterWrite.getMilliseconds(), TimeUnit.MILLISECONDS);
                        }
                        if (!Strings.isEmpty(key.expireAfterAccess())) {
                            Duration expireAfterAccess = Duration.valueOf(key.expireAfterAccess());
                            builder.expireAfterAccess(expireAfterAccess.getMilliseconds(), TimeUnit.MILLISECONDS);
                        }
                        boolean recordStats = key.recordStats();
                        if (recordStats) {
                            builder.recordStats();
                        }
                        long maximumSize = key.maximumSize();
                        if (maximumSize != -1) {
                            builder.maximumSize(maximumSize);
                        }
                        return builder.build();
                    }
                }
        );
    }

    /**
     * Gets cache for
     *
     * @param cacheable annotation
     * @return cache for the annotation
     * @throws java.util.concurrent.ExecutionException
     */
    public Cache<String, Object> get(CacheReturnValue cacheable) throws ExecutionException {
        return cacheCache.get(cacheable);
    }

    /**
     * @return the caches as a map
     */
    public Map<CacheReturnValue, Cache<String, Object>> cachesAsMap() {
        return cacheCache.asMap();
    }

    /**
     * @return iterator over caches
     */
    @Override
    public Iterator<Cache<String, Object>> iterator() {
        return cachesAsMap().values().iterator();
    }
}
