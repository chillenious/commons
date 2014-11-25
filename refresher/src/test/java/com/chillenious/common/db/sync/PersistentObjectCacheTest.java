package com.chillenious.common.db.sync;

import com.chillenious.common.ShutdownHooks;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PersistentObjectCacheTest {

    @Test
    public void testApi() {

        ShutdownHooks shutdownHooks = new ShutdownHooks();
        PersistentObjectCache<PersistentObject> cache =
                new PersistentObjectCache<>(null, shutdownHooks, false, true);

        try {
            cache.getSorter("foo");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            cache.getIndexer("foo");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        shutdownHooks.runShutdownHooks();
    }

    @Test
    public void testDirectCacheUseWithSorts()
            throws InterruptedException, ExecutionException, TimeoutException {

        ShutdownHooks shutdownHooks = new ShutdownHooks();
        PersistentObjectCache<Bam> cache =
                new PersistentObjectCache<>(null, shutdownHooks, false, true);
        cache.put(new Bam(1L, "Bambading"));
        Bam fromCache = cache.get(1L);
        Assert.assertNotNull(fromCache);
        Assert.assertEquals("Bambading", fromCache.getName());
        cache.put(new Bam(2L, "Rengkedeng"));
        cache.put(new Bam(3L, "Doioing"));
        cache.put(new Bam(4L, "Aiaiaiai"));
        Future<Integer> f = cache.addSort("name", (object, isNew) ->
                SortKey.forObject(object).create(object.getName()));
        int nbrSorted = f.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(nbrSorted, 4);
        Iterator<Bam> i = cache.values("name").iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals("Aiaiaiai", i.next().getName());
        Assert.assertEquals("Bambading", i.next().getName());
        Assert.assertEquals("Doioing", i.next().getName());
        Assert.assertEquals("Rengkedeng", i.next().getName());
        Assert.assertFalse(i.hasNext());

        cache.put(new Bam(5L, "Abababai"));
        cache.put(new Bam(6L, "Eieieiei"));

        i = cache.values("name").iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals("Abababai", i.next().getName());
        Assert.assertEquals("Aiaiaiai", i.next().getName());
        Assert.assertEquals("Bambading", i.next().getName());
        Assert.assertEquals("Doioing", i.next().getName());
        Assert.assertEquals("Eieieiei", i.next().getName());
        Assert.assertEquals("Rengkedeng", i.next().getName());
        Assert.assertFalse(i.hasNext());

        cache.remove(1L);
        cache.remove(3L);
        cache.remove(5L);

        Assert.assertNull(cache.get(3L));

        i = cache.values("name").iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals("Aiaiaiai", i.next().getName());
        Assert.assertEquals("Eieieiei", i.next().getName());
        Assert.assertEquals("Rengkedeng", i.next().getName());
        Assert.assertFalse(i.hasNext());

        shutdownHooks.runShutdownHooks();
    }

    @Test
    public void testDirectCacheUseWithIndexers()
            throws InterruptedException, ExecutionException, TimeoutException {

        ShutdownHooks shutdownHooks = new ShutdownHooks();
        PersistentObjectCache<Bam> cache =
                new PersistentObjectCache<>(null, shutdownHooks, false, true);

        Bam first = new Bam(1, "foo");
        Bam second = new Bam(2, "foo");
        Bam third = new Bam(3, "foo");
        Bam fourth = new Bam(4, "bar");
        Bam fifth = new Bam(5, "bar");
        cache.put(first);
        cache.put(second);
        cache.put(third);
        cache.put(fourth);
        cache.put(fifth);

        Future<Integer> f = cache.addIndex("name", Bam::getName);
        int nbrSorted = f.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(nbrSorted, 5);

        Set<Bam> fooSet = cache.getIndexed("name", "foo");
        Assert.assertNotNull(fooSet);
        Assert.assertEquals(3, fooSet.size());
        Assert.assertTrue(fooSet.contains(first));
        Assert.assertTrue(fooSet.contains(second));
        Assert.assertTrue(fooSet.contains(third));
        Assert.assertFalse(fooSet.contains(fourth));

        Set<Bam> barSet = cache.getIndexed("name", "bar");
        Assert.assertNotNull(barSet);
        Assert.assertEquals(2, barSet.size());
        Assert.assertTrue(barSet.contains(fourth));
        Assert.assertTrue(barSet.contains(fifth));
        Assert.assertFalse(barSet.contains(first));

        f = cache.addIndex("oneone", Bam::getOneToOneFk);
        nbrSorted = f.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(nbrSorted, 5);

        Assert.assertEquals(first,
                cache.getIndexedSingle("oneone", first.getOneToOneFk()));
        Assert.assertEquals(fifth,
                cache.getIndexedSingle("oneone", fifth.getOneToOneFk()));

        shutdownHooks.runShutdownHooks();
    }
}
