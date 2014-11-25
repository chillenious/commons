package com.chillenious.common.caching;

import com.chillenious.common.Bootstrap;
import com.chillenious.common.Settings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Test for the method cache.
 */
@SuppressWarnings("unchecked")
public class MethodResultsCacheTest {

    public static class CacheTester {

        @CacheReturnValue
        public int methodWithCaching(int x, int y) {
            return add(x, y);
        }

        public int methodWithoutCaching(int x, int y) {
            return add(x, y);
        }

        private int add(int x, int y) {
            return x + y;
        }

        @CacheReturnValue
        public Object returnsNull() {
            return null;
        }
    }

    private Bootstrap bootstrap;

    @Inject
    private CacheTester cacheTester;

    @Inject
    private MethodResultsCaches caches;

    @Before
    public void setup() throws Exception {

        Properties p = new Properties();
        Settings settings = Settings.builder()
                .add("methodcache.enabled", "true")
                .build();
        bootstrap = new Bootstrap(settings, new MethodResultsCachingModule(
                settings, "com.chillenious.common.caching"));
        bootstrap.getInjector().injectMembers(this);
    }

    @After
    public void teardown() throws Exception {
        bootstrap.shutdown();
    }

    @Test
    public void testMethodCache() throws NoSuchMethodException, ExecutionException {

        CacheReturnValue annotation = getCacheAnnotation(
                CacheTester.class, cacheTester, "methodWithCaching", Integer.TYPE, Integer.TYPE);

        int numberOfCalls = 1000;
        for (int i = 0; i < numberOfCalls; i++) {
            cacheTester.methodWithoutCaching(1, 1);
        }

        for (int i = 0; i < numberOfCalls; i++) {
            cacheTester.methodWithCaching(1, 1);
        }

        Cache<String, Object> cache = caches.get(annotation);
        Assert.assertNotNull(cache); // should be there now

        CacheStats stats = cache.stats();
        long hitCount = stats.hitCount();
        Assert.assertEquals("cache hits should equal number of invocations minus one (method is cacheable and " +
                        "parameters are the same for all invocations)",
                numberOfCalls - 1, hitCount
        );

        for (int i = 0; i < numberOfCalls; i++) {
            cacheTester.methodWithCaching(numberOfCalls + 1, i % (numberOfCalls / 2));
        }
        stats = cache.stats();
        Assert.assertEquals(numberOfCalls / 2 + hitCount, stats.hitCount());

        cacheTester.returnsNull(); // this should not yield an exception
    }

    private CacheReturnValue getCacheAnnotation(Class cls, Object o, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = cls.getMethod(methodName, parameterTypes);
            CacheReturnValue annotation = method.getAnnotation(CacheReturnValue.class);
            Assert.assertNotNull(annotation);
            return annotation;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
