package com.chillenious.common.util;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Enables you to memoize an operation using a cache key (type) and parameters (keys).
 * The 'cache' lives for the duration of the JVM instance, so this should be used only
 * for things that are relatively expensive to compute but won't change for the duration
 * of the JVM instance (e.g. introspection ops).
 */
public final class Memoize {

    private static ConcurrentMap<String, Map<Object, Object>> CACHES =
            new ConcurrentHashMap<>();

    /**
     * Execute operation or return cached result if it had been run before.
     *
     * @param operation The expensive operation.
     * @param type      The cache type to be used.
     * @param keys      The cache keys.
     * @return The cached value or the outcome of the cached operation.
     */
    @SuppressWarnings("unchecked")
    public static <V> V get(Supplier<V> operation, String type, Object... keys) {

        Map<Object, Object> cache;
        cache = CACHES.get(type);
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            CACHES.put(type, cache);
        }

        Object key = key(keys);
        Object result = cache.get(key);

        if (result == null) {
            result = operation.get();
            cache.put(key, result == null ? NULL : result);
        }

        return (V) (result == NULL ? null : result);
    }

    /**
     * A <code>null</code> placeholder to be put in {@link java.util.concurrent.ConcurrentHashMap}.
     */
    private static final Object NULL = new Object();

    /**
     * Create a single-value or multi-value key for caching.
     */
    private static Object key(final Object... key) {
        if (key == null || key.length == 0) {
            return key;
        }
        if (key.length == 1) {
            return key[0];
        }
        return new Key(key);
    }

    /**
     * A multi-value key for caching.
     */
    private static class Key {

        private final Object[] key;

        Key(Object[] key) {
            this.key = key;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                return Arrays.equals(key, ((Key) obj).key);
            }
            return false;
        }

        @Override
        public String toString() {
            return Arrays.asList(key).toString();
        }
    }
}