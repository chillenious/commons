package com.chillenious.common.caching;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Methods annotated with this have their return value cached. Methods that have the same annotation
 * (looking at the parameters that are set) share the same cache.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface CacheReturnValue {

    /**
     * Enable the accumulation of {@link com.google.common.cache.CacheStats} during the operation of the cache. Without this
     * {@link com.google.common.cache.Cache#stats} will return zero for all statistics. Note that recording stats requires
     * bookkeeping to be performed with each operation, and thus imposes a performance penalty on
     * cache operation.
     * <p>
     * Default is true
     */
    boolean recordStats() default true;

    /**
     * Specifies that each entry should be automatically removed from the cache once a fixed duration
     * has elapsed after the entry's creation, or the most recent replacement of its value.
     * <p>
     * The value is the duration the length of time after an entry is created that it should
     * be automatically removed, as a string that can be parsed by {@link com.chillenious.common.util.Duration}.
     * <p>
     * Default is one minute.
     */
    String expireAfterWrite() default "1 minute";

    /**
     * Specifies that each entry should be automatically removed from the cache once a fixed duration
     * has elapsed after the entry's creation, the most recent replacement of its value, or its last
     * access. Access time is reset by all cache read and write operations (including
     * {@code Cache.asMap().get(Object)} and {@code Cache.asMap().put(K, V)}), but not by operations
     * on the collection-views of {@link com.google.common.cache.Cache#asMap}.
     */
    String expireAfterAccess() default "";

    /**
     * Specifies the maximum number of entries the cache may contain. Note that the cache <b>may evict
     * an entry before this limit is exceeded</b>. As the cache size grows close to the maximum, the
     * cache evicts entries that are less likely to be used again. For example, the cache may evict an
     * entry because it hasn't been used recently or very often.
     */
    long maximumSize() default -1;
}
