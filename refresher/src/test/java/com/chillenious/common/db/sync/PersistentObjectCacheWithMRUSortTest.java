package com.chillenious.common.db.sync;

import com.google.inject.Inject;
import com.chillenious.common.Bootstrap;
import com.chillenious.common.Settings;
import com.chillenious.common.util.CurrentTime;
import com.chillenious.common.util.Duration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PersistentObjectCacheWithMRUSortTest {

    private Bootstrap bootstrap;

    @Inject
    private PersistentObjectCacheBuilder builder;

    @Before
    public void setup() {
        bootstrap = new Bootstrap(Settings.builder().build());
        bootstrap.getInjector().injectMembers(this);
    }

    @After
    public void teardown() {
        CurrentTime.reset();
    }

    @Test
    public void testDirectCacheUse()
            throws InterruptedException, ExecutionException, TimeoutException {

        PersistentObjectCacheWithMRUSort<Bam> cache =
                builder.withRefresher(new NoopRefresher<Bam>())
                        .withRefreshEvery(Duration.minutes(5))
                        .withMruSort().build();

        Bam first = new Bam(1, "first");
        Bam second = new Bam(2, "second");
        Bam third = new Bam(3, "third");

        CurrentTime.freezeAt(1000);
        cache.put(first);

        CurrentTime.freezeAt(2000);
        cache.put(second);

        CurrentTime.freezeAt(3000);
        cache.put(third);

        Iterator<Bam> i = cache.valuesByMostRecentlyUsed().iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(third, i.next());
        Assert.assertEquals(second, i.next());
        Assert.assertEquals(first, i.next());

        CurrentTime.freezeAt(4000);

        cache.touch(second);
        Assert.assertEquals(second, cache.valuesByMostRecentlyUsed().iterator().next());

        CurrentTime.freezeAt(5000);

        cache.touch(first);
        i = cache.valuesByMostRecentlyUsed().iterator();
        Assert.assertEquals(first, i.next());
        Assert.assertEquals(second, i.next());
        Assert.assertEquals(third, i.next());
    }

    @Test
    public void testDupHandling() {

        // test that objects that should be marked as duplicates (same hashcode/ equals) are not added
        // multiple times in the MRU cache
        PersistentObjectCacheWithMRUSort<Bam> cache =
                builder.withRefresher(new NoopRefresher<Bam>())
                        .withMruSort().build();

        Bam first = new Bam(1, "first");
        Bam firstDuplicate = new Bam(1, "second"); // note the same id

        CurrentTime.freezeAt(1000);
        cache.put(first);
        CurrentTime.freezeAt(2000);
        cache.put(firstDuplicate);

        Assert.assertEquals(1, cache.valuesByMostRecentlyUsed().size());
    }

    @Test
    public void testPromoteAlways() {

        // test that new as well as updated objects that are put in the cache are promoted
        PersistentObjectCacheWithMRUSort<Bam> cache =
                builder.withRefresher(new NoopRefresher<Bam>())
                        .withMruSort().promoteWhenNewOrUpdated().build();

        Bam first = new Bam(1, "first");
        Bam second = new Bam(2, "second");

        CurrentTime.freezeAt(1000);
        cache.put(first);
        CurrentTime.freezeAt(2000);
        cache.put(second);
        CurrentTime.freezeAt(3000);
        cache.put(first); // this time, because of the different strategy, it should always
        // update the order, and hence have first in front again

        Assert.assertEquals(first, cache.valuesByMostRecentlyUsed().iterator().next());
    }

    @Test
    public void testPromoteOnlyNew() {
        // test that only new objects that are put in the cache are promoted
        PersistentObjectCacheWithMRUSort<Bam> cache =
                builder.withRefresher(new NoopRefresher<Bam>())
                        .withMruSort().promoteWhenNewOnly().build();

        Bam first = new Bam(1, "first");
        Bam second = new Bam(2, "second");

        CurrentTime.freezeAt(1000);
        cache.put(first);
        CurrentTime.freezeAt(2000);
        cache.put(second);
        CurrentTime.freezeAt(3000);
        cache.put(first); // this is an update, so second should still be in front

        Assert.assertEquals(second, cache.valuesByMostRecentlyUsed().iterator().next());
    }
}
